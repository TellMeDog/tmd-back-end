import argparse
import csv
import hashlib
import json
import os
import re
import tempfile
import unicodedata
from pathlib import Path


RAW_COLUMNS = [
    "acmpy_type_cd",
    "acmpy_psbl_cpam",
    "acmpy_need_mtr",
    "rela_acdnt_risk_mtr",
    "rela_poses_fclty",
    "rela_frnsh_prdlst",
    "rela_purc_prdlst",
    "rela_rntl_prdlst",
    "etc_acmpy_info",
]

POLICY_COLUMNS = [
    "place_id",
    "access_scope",
    "all_breeds_allowed",
    "dangerous_breed_allowed",
    "dangerous_breed_allowed_condition",
    "max_weight_kg",
    "weight_limit_type",
    "leash_required",
    "muzzle_required",
    "kennel_required",
    "advance_inquiry_required",
    "max_pet_count",
    "default_policy",
]

EXPLICIT_ALL_BREEDS = re.compile(r"전\s*견종|모든\s*견종|전체\s*견종")
LIMITED_BREEDS = re.compile(
    r"맹견\s*(?:은|의\s*경우)?\s*(?:제외|불가|금지)|"
    r"일부\s*견종|소형견|중소형견|중형견|대형견|안내견|특정\s*견종|"
    r"견종에\s*따라|견종별"
)
DANGEROUS_EXCLUDED = re.compile(
    r"맹견\s*(?:은|의\s*경우)?\s*(?:제외|X)|"
    r"맹견.{0,18}(?:입장|출입|동반|입실)\s*(?:불가|제한|금지)|"
    r"(?:입장|출입|동반|입실)\s*제한\s*반려견.{0,80}맹견"
)
DANGEROUS_MUZZLE = re.compile(r"맹견.{0,35}입마개|입마개.{0,35}맹견")
GENERAL_DENIAL = re.compile(
    r"^(?:반려동물|반려견|애완동물)?\s*(?:동반|입장|출입)?\s*(?:불가|금지)$|"
    r"(?:반려동물|반려견|애완동물)\s*(?:동반|입장|출입)\s*(?:불가|금지)"
)
PARTIAL_ACCESS = re.compile(
    r"실내.{0,15}(?:동반|입장|출입)?\s*(?:불가|제한)|내부.{0,15}(?:동반|입장|출입)?\s*(?:불가|제한)|"
    r"외부.{0,8}(?:만|한정).{0,8}(?:가능|이용)|야외.{0,8}(?:만|한정).{0,8}(?:가능|이용)|"
    r"일부\s*(?:구역|시설)|지정.{0,8}(?:구역|장소)|산책로만|테라스만|"
    r"(?:전시관|체험시설|캠핑장|객실|카라반|방갈로|수영장|해수욕장|레스토랑|식당|카페|매장|화장실|휴게시설|부대시설|식음업장|선실).{0,18}(?:동반|입장|입실|출입|이용)?\s*(?:불가|제한|금지)|"
    r"입수\s*(?:불가|제한|금지)"
)
WEIGHT_PATTERN = re.compile(r"(?<!\d)(\d+(?:\.\d+)?)\s*(?:kg|㎏|킬로그램|킬로)", re.I)
COUNT_PATTERN = re.compile(r"(?:최대\s*)?(\d+)\s*마리(?:까지|만)?\s*(?:동반|입장|출입|가능|허용)")
SPECIAL_ASSISTANCE_ONLY = re.compile(
    r"^(?:(?:시각\s*장애인|맹인|장애우)\s*)?(?:안내견|보조견)(?:만)?(?:\s*(?:가능|가))?$|"
    r"^불가\s*\(\s*보조견만\s*가능\s*\)$|"
    r"^(?:장애우\s*)?안내견만\s*이용가능$"
)
VARIABLE_ACCESS = re.compile(r"점포마다\s*다름|매장.{0,15}(?:상이|정책)|개별\s*매장")
AFFIRMATIVE_ACCESS = re.compile(
    r"동반\s*(?:입장|출입)?\s*가능|입장\s*가능|출입\s*가능|이용객에\s*한하여\s*허용|"
    r"^가능\s*\(|반려견\s*\([^)]*kg[^)]*\)|"
    r"(?:소형견|중소형견|중형견|대형견)(?:만|\s|.*가능)"
)

NON_COMPARABLE_PATTERNS = {
    "vaccination": re.compile(r"예방\s*접종|백신|접종증"),
    "stroller": re.compile(r"유모차"),
    "manner_belt": re.compile(r"매너\s*벨트|기저귀"),
    "waste_bag": re.compile(r"배변\s*봉투|배설물|배변\s*처리"),
    "registration": re.compile(r"동물\s*등록|등록증"),
    "health_or_behavior": re.compile(r"질병|발정|공격성|사나운|사납|짖음|중성화"),
    "age_or_sex": re.compile(r"\d+\s*(?:개월|세)\s*(?:이상|이하|미만)|수컷|암컷"),
    "guardian_condition": re.compile(r"보호자.{0,15}(?:동반|책임|성인|나이)|\d+세\s*이상.{0,10}보호자"),
    "site_variability": re.compile(r"현지\s*사정|운영\s*정책.{0,10}변동|혼잡도|상황에\s*따라|현장.{0,8}판단"),
    "breed_not_representable": re.compile(r"일부\s*견종|소형견|중소형견|중형견|대형견|안내견|특정\s*견종|견종별|견종에\s*따라"),
}


def clean(value):
    if value is None:
        return ""
    return unicodedata.normalize("NFKC", str(value)).strip()


def source_text(value):
    value = clean(value)
    if len(value) >= 2 and value[0] == "'" and value[1] in "=+-@":
        return value[1:]
    return value


def excel_safe(value):
    value = clean(value)
    if value.startswith(("=", "+", "-", "@")):
        return "'" + value
    return value


def nullable_bool(value):
    if value is True:
        return "1"
    if value is False:
        return "0"
    return ""


def format_number(value):
    if value is None:
        return ""
    if float(value).is_integer():
        return str(int(value))
    return str(value).rstrip("0").rstrip(".")


def split_sentences(text):
    return [
        part.strip()
        for part in re.split(r"[\n\r]|(?<=[.!?])\s*|\s*-\s*|\s*/\s*|※", text)
        if part.strip()
    ]


def source_fingerprint(row):
    source = {"status": source_text(row.get("status") or row.get("source_status"))}
    source.update({column: source_text(row.get(column)) for column in RAW_COLUMNS})
    payload = json.dumps(source, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def extract_weight_limit(text):
    candidates = []
    ignored = []
    for sentence in split_sentences(text):
        matches = [float(value) for value in WEIGHT_PATTERN.findall(sentence)]
        if not matches:
            continue
        admission_words = re.search(r"동반|입장|입실|출입|가능|허용|불가|제한|전용|객실당", sentence)
        equipment_only = re.search(r"입마개|이동장|켄넬|목줄", sentence) and not admission_words
        if equipment_only:
            ignored.extend(matches)
            continue
        for value in matches:
            number = re.escape(format_number(value))
            unit = r"\s*(?:kg|㎏|킬로그램|킬로)"
            if re.search(rf"{number}{unit}\s*미만", sentence, re.I):
                candidates.append((value, "LESS_THAN"))
            elif re.search(rf"{number}{unit}\s*(?:이하|이내)", sentence, re.I):
                candidates.append((value, "LESS_THAN_OR_EQUAL"))
            elif re.search(rf"{number}{unit}\s*이상", sentence, re.I) and re.search(r"불가|제한", sentence):
                candidates.append((value, "LESS_THAN"))
            elif re.search(rf"{number}{unit}\s*초과", sentence, re.I) and re.search(r"불가|제한", sentence):
                candidates.append((value, "LESS_THAN_OR_EQUAL"))
            elif re.search(rf"{number}{unit}.{{0,18}}(?:이상|초과)\s*(?:불가|제한)", sentence, re.I):
                candidates.append((value, "LESS_THAN"))
            elif admission_words:
                candidates.append((value, "UNKNOWN"))
    return candidates, ignored


def classify(row):
    values = {column: source_text(row.get(column)) for column in RAW_COLUMNS}
    status = source_text(row.get("status"))
    combined = "\n".join(value for value in values.values() if value)
    decision_text = "\n".join(
        values[column]
        for column in ("acmpy_psbl_cpam", "acmpy_need_mtr", "rela_acdnt_risk_mtr", "etc_acmpy_info")
        if values[column]
    )
    rules, review_reasons, yellow_reasons = [], [], []

    if status == "NO_DATA":
        return {
            "place_id": str(row["place_id"]),
            "access_scope": "UNKNOWN",
            "all_breeds_allowed": "",
            "dangerous_breed_allowed": "",
            "dangerous_breed_allowed_condition": "",
            "max_weight_kg": "",
            "weight_limit_type": "UNKNOWN",
            "leash_required": "",
            "muzzle_required": "",
            "kennel_required": "",
            "advance_inquiry_required": "",
            "max_pet_count": "",
            "default_policy": "",
        }, ["NO_DATA=>UNKNOWN/GREY"], [], []

    type_code = values["acmpy_type_cd"]
    capacity = values["acmpy_psbl_cpam"]
    if "일부구역" in type_code or "일부 구역" in type_code:
        access_scope = "PARTIAL"
        rules.append("acmpy_type_cd=>PARTIAL")
    elif "전구역" in type_code or "전 구역" in type_code:
        access_scope = "ALL"
        rules.append("acmpy_type_cd=>ALL")
    elif SPECIAL_ASSISTANCE_ONLY.fullmatch(capacity) or SPECIAL_ASSISTANCE_ONLY.fullmatch(values["etc_acmpy_info"]):
        access_scope = "NONE"
        rules.append("assistance_dog_only=>NONE")
    elif VARIABLE_ACCESS.search(decision_text):
        access_scope = "PARTIAL"
        yellow_reasons.append("variable_access")
        rules.append("variable_access=>PARTIAL")
    elif AFFIRMATIVE_ACCESS.search(decision_text) or re.fullmatch(r"(?:소형견|중소형견|중형견|대형견|반려견)", capacity):
        access_scope = "ALL"
        rules.append("explicit_permission=>ALL")
    elif GENERAL_DENIAL.search(decision_text):
        access_scope = "NONE"
        rules.append("explicit_denial=>NONE")
    else:
        access_scope = "UNKNOWN"
        rules.append("scope_missing=>UNKNOWN")

    if PARTIAL_ACCESS.search(decision_text):
        if access_scope == "ALL":
            review_reasons.append("scope_conflict: type=ALL but detailed text limits areas")
        access_scope = "PARTIAL"
        yellow_reasons.append("partial_access")
        rules.append("detailed_area_limit=>PARTIAL")
    if GENERAL_DENIAL.fullmatch(capacity):
        if access_scope in {"ALL", "PARTIAL"}:
            review_reasons.append("scope_conflict: type permits access but capacity says denied")
        access_scope = "NONE"
        rules.append("capacity_denial=>NONE")
    if access_scope == "PARTIAL":
        yellow_reasons.append("partial_access")

    if LIMITED_BREEDS.search(decision_text) or WEIGHT_PATTERN.search(decision_text):
        all_breeds = False
        rules.append("breed_or_weight_limit=>allBreeds=0")
    elif EXPLICIT_ALL_BREEDS.search(decision_text):
        all_breeds = True
        rules.append("explicit_all_breeds=>allBreeds=1")
    else:
        all_breeds = None

    dangerous_excluded = any(DANGEROUS_EXCLUDED.search(sentence) for sentence in split_sentences(decision_text))
    if dangerous_excluded:
        dangerous_allowed, dangerous_condition = False, ""
        rules.append("dangerous_excluded=>dangerous=0")
    elif DANGEROUS_MUZZLE.search(decision_text):
        dangerous_allowed, dangerous_condition = True, "MUZZLE"
        rules.append("dangerous_muzzle=>dangerous=1/MUZZLE")
    elif all_breeds is True:
        dangerous_allowed, dangerous_condition = True, ""
        rules.append("all_breeds=>dangerous=1")
    else:
        dangerous_allowed, dangerous_condition = None, ""

    weight_candidates, ignored_weights = extract_weight_limit(decision_text)
    unique_weights = sorted(set(value for value, _ in weight_candidates))
    max_weight = min(unique_weights) if unique_weights else None
    weight_type = "UNKNOWN"
    if max_weight is not None:
        known_types = {kind for value, kind in weight_candidates if value == max_weight} - {"UNKNOWN"}
        if len(known_types) == 1:
            weight_type = next(iter(known_types))
        elif len(known_types) > 1:
            weight_type = "LESS_THAN"
            review_reasons.append("conflicting_weight_boundaries")
            yellow_reasons.append("ambiguous_weight_boundary")
        else:
            review_reasons.append("weight_limit_without_boundary")
            yellow_reasons.append("ambiguous_weight_boundary")
        rules.append(f"weight={format_number(max_weight)}/{weight_type}")
        if len(unique_weights) > 1:
            review_reasons.append("multiple_weight_values: " + ",".join(format_number(v) for v in unique_weights))
            yellow_reasons.append("multiple_weight_values")
    if ignored_weights:
        rules.append("ignored_equipment_weight=" + ",".join(format_number(v) for v in sorted(set(ignored_weights))))

    need_text = values["acmpy_need_mtr"]
    leash_required = True if re.search(r"목줄|리드\s*줄", decision_text) else None
    if leash_required:
        rules.append("leash_text=>leash=1")
    muzzle_required = None
    if "입마개" in need_text:
        muzzle_required = True
        rules.append("required_items_muzzle=>muzzle=1")
    elif any("입마개" in sentence and "맹견" not in sentence for sentence in split_sentences(decision_text)):
        muzzle_required = True
        rules.append("general_muzzle_text=>muzzle=1")
    kennel_required = True if re.search(r"이동장|켄넬|케이지|캐리어", decision_text) else None
    if kennel_required:
        rules.append("carrier_text=>kennel=1")
    advance_inquiry = True if re.search(r"사전\s*문의|전화\s*문의|문의\s*(?:필수|후|바람|요망)|확인\s*후", decision_text) else None
    if advance_inquiry:
        rules.append("inquiry_text=>advanceInquiry=1")
        yellow_reasons.append("advance_inquiry")

    unique_counts = sorted(set(int(value) for value in COUNT_PATTERN.findall(decision_text)))
    max_pet_count = min(unique_counts) if unique_counts else None
    if max_pet_count is not None:
        rules.append(f"max_pet_count={max_pet_count}")
        yellow_reasons.append("group_pet_count")
    if len(unique_counts) > 1:
        review_reasons.append("multiple_pet_counts: " + ",".join(map(str, unique_counts)))
    for label, pattern in NON_COMPARABLE_PATTERNS.items():
        if pattern.search(decision_text):
            yellow_reasons.append(label)
    if re.search(r"자유\s*이용", need_text):
        if leash_required or muzzle_required or kennel_required:
            review_reasons.append("free_use_conflicts_with_required_equipment")
        rules.append("free_use_seen")
    if access_scope == "UNKNOWN" and combined:
        review_reasons.append("scope_unknown_with_source_text")

    rules = list(dict.fromkeys(rules))
    review_reasons = list(dict.fromkeys(review_reasons))
    yellow_reasons = list(dict.fromkeys(yellow_reasons))
    if review_reasons:
        yellow_reasons = list(dict.fromkeys([*yellow_reasons, "manual_review"]))
    if access_scope == "NONE":
        yellow_reasons = []
        rules.append("definitive_denial=>defaultPolicy=NULL")

    return {
        "place_id": str(row["place_id"]),
        "access_scope": access_scope,
        "all_breeds_allowed": nullable_bool(all_breeds),
        "dangerous_breed_allowed": nullable_bool(dangerous_allowed),
        "dangerous_breed_allowed_condition": dangerous_condition,
        "max_weight_kg": format_number(max_weight),
        "weight_limit_type": weight_type,
        "leash_required": nullable_bool(leash_required),
        "muzzle_required": nullable_bool(muzzle_required),
        "kennel_required": nullable_bool(kennel_required),
        "advance_inquiry_required": nullable_bool(advance_inquiry),
        "max_pet_count": "" if max_pet_count is None else str(max_pet_count),
        "default_policy": "YELLOW" if yellow_reasons else "",
    }, rules, review_reasons, yellow_reasons


def load_jsonl(path):
    with path.open("r", encoding="utf-8") as source_file:
        rows = [json.loads(line) for line in source_file if line.strip()]
    ids = [str(row.get("place_id", "")) for row in rows]
    if not rows or "" in ids or len(ids) != len(set(ids)):
        raise ValueError("JSONL은 비어 있지 않고 고유한 place_id를 가져야 합니다.")
    return rows


def load_csv(path):
    if path is None:
        return {}
    with path.open("r", encoding="utf-8-sig", newline="") as csv_file:
        rows = list(csv.DictReader(csv_file))
    return {clean(row.get("place_id")): row for row in rows if clean(row.get("place_id"))}


def policy_only(row):
    return {column: clean(row.get(column)) for column in POLICY_COLUMNS}


def build_review(row, policy, suggested, rules, review_reasons, yellow_reasons, change_status, previous=None):
    previous = previous or {}
    required = bool(review_reasons)
    review = dict(policy)
    review.update({
        "content_id": clean(row.get("content_id")),
        "source_status": clean(row.get("status")),
        "source_fingerprint": source_fingerprint(row),
        "change_status": change_status,
        "review_required": "1" if required else "0",
        "review_status": "PENDING" if required else clean(previous.get("review_status")) or "AUTO",
        "review_note": clean(previous.get("review_note")),
        "review_reasons": " | ".join(review_reasons),
        "yellow_reasons": " | ".join(yellow_reasons),
        "matched_rules": " | ".join(rules),
    })
    for column in POLICY_COLUMNS[1:]:
        review[f"suggested_{column}"] = suggested[column]
    review.update({column: excel_safe(row.get(column)) for column in RAW_COLUMNS})
    return review


def merge_rows(source_rows, existing_policies=None, existing_reviews=None, full=False):
    existing_policies = existing_policies or {}
    existing_reviews = existing_reviews or {}
    policies, reviews = [], []
    current_ids = set()

    for row in source_rows:
        place_id = str(row["place_id"])
        current_ids.add(place_id)
        suggested, rules, reasons, yellow_reasons = classify(row)
        previous_policy = existing_policies.get(place_id)
        previous_review = existing_reviews.get(place_id, {})

        if full or previous_policy is None:
            policy = suggested
            change_status = "FULL_REGENERATED" if full else "NEW"
        else:
            policy = policy_only(previous_policy)
            old_fingerprint = clean(previous_review.get("source_fingerprint"))
            if not old_fingerprint and previous_review:
                old_fingerprint = source_fingerprint(previous_review)
            if old_fingerprint and old_fingerprint == source_fingerprint(row):
                change_status = "UNCHANGED"
                reasons = [part.strip() for part in clean(previous_review.get("review_reasons")).split("|") if part.strip()]
                yellow_reasons = [part.strip() for part in clean(previous_review.get("yellow_reasons")).split("|") if part.strip()]
                rules = [part.strip() for part in clean(previous_review.get("matched_rules")).split("|") if part.strip()]
            else:
                change_status = "SOURCE_CHANGED"
                reasons = list(dict.fromkeys(["source_changed", *reasons]))

        policies.append(policy)
        reviews.append(build_review(
            row, policy, suggested, rules, reasons, yellow_reasons, change_status, previous_review
        ))

    if not full:
        for place_id in sorted(set(existing_policies) - current_ids, key=lambda value: int(value)):
            policy = policy_only(existing_policies[place_id])
            previous = existing_reviews.get(place_id, {})
            placeholder = {
                "place_id": place_id,
                "content_id": previous.get("content_id", ""),
                "status": previous.get("source_status", ""),
            }
            placeholder.update({column: source_text(previous.get(column)) for column in RAW_COLUMNS})
            policies.append(policy)
            reviews.append(build_review(
                placeholder,
                policy,
                policy,
                ["existing_policy_preserved"],
                ["source_missing"],
                [],
                "SOURCE_MISSING",
                previous,
            ))

    paired = sorted(zip(policies, reviews), key=lambda pair: int(pair[0]["place_id"]))
    return [pair[0] for pair in paired], [pair[1] for pair in paired]


def write_csv_atomic(path, fieldnames, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", encoding="utf-8-sig", newline="", delete=False, dir=path.parent) as temp_file:
        writer = csv.DictWriter(temp_file, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)
        temporary_path = Path(temp_file.name)
    os.replace(temporary_path, path)


def validate(policies, reviews):
    ids = [row["place_id"] for row in policies]
    if len(ids) != len(set(ids)):
        raise ValueError("정책 결과에 중복 place_id가 있습니다.")
    valid_scopes = {"ALL", "PARTIAL", "NONE", "UNKNOWN"}
    valid_weights = {"LESS_THAN", "LESS_THAN_OR_EQUAL", "UNKNOWN"}
    for policy in policies:
        if policy["access_scope"] not in valid_scopes:
            raise ValueError(f"잘못된 access_scope: {policy}")
        if policy["weight_limit_type"] not in valid_weights:
            raise ValueError(f"잘못된 weight_limit_type: {policy}")
        for column in ("all_breeds_allowed", "dangerous_breed_allowed", "leash_required", "muzzle_required", "kennel_required", "advance_inquiry_required"):
            if policy[column] not in {"", "0", "1"}:
                raise ValueError(f"잘못된 Boolean 값: {column}={policy[column]}")
        if policy["default_policy"] not in {"", "YELLOW"}:
            raise ValueError(f"잘못된 default_policy: {policy}")
        if policy["dangerous_breed_allowed_condition"] and policy["dangerous_breed_allowed"] != "1":
            raise ValueError(f"맹견 허용 여부와 조건이 모순됩니다: {policy}")
        if policy["access_scope"] == "NONE" and policy["default_policy"]:
            raise ValueError(f"확정 출입 불가 장소에 defaultPolicy가 있습니다: {policy}")
    if len(policies) != len(reviews):
        raise ValueError("정책 CSV와 검토 CSV의 행 수가 다릅니다.")


def parse_args():
    parser = argparse.ArgumentParser(description="PlacePetInfo JSONL을 PlacePetPolicy CSV로 변환합니다.")
    parser.add_argument("--source", type=Path, required=True, help="export_place_pet_info.ps1로 생성한 JSONL")
    parser.add_argument("--output-dir", type=Path, required=True, help="결과 CSV 디렉터리")
    parser.add_argument("--existing-policy", type=Path, help="수동 수정값을 보존할 기존 정책 CSV")
    parser.add_argument("--existing-review", type=Path, help="원문 변경을 판별할 기존 검토 CSV")
    parser.add_argument("--full", action="store_true", help="기존 정책을 무시하고 전체를 다시 생성")
    return parser.parse_args()


def main():
    args = parse_args()
    source_rows = load_jsonl(args.source)
    existing_policies = {} if args.full else load_csv(args.existing_policy)
    existing_reviews = {} if args.full else load_csv(args.existing_review)
    policies, reviews = merge_rows(source_rows, existing_policies, existing_reviews, args.full)
    validate(policies, reviews)

    policy_path = args.output_dir / "place_pet_policy.csv"
    review_path = args.output_dir / "place_pet_policy_review.csv"
    suggested_columns = [f"suggested_{column}" for column in POLICY_COLUMNS[1:]]
    review_columns = POLICY_COLUMNS + [
        "content_id",
        "source_status",
        "source_fingerprint",
        "change_status",
        "review_required",
        "review_status",
        "review_note",
        "review_reasons",
        "yellow_reasons",
        "matched_rules",
        *suggested_columns,
        *RAW_COLUMNS,
    ]
    write_csv_atomic(policy_path, POLICY_COLUMNS, policies)
    write_csv_atomic(review_path, review_columns, reviews)

    counts = {}
    for review in reviews:
        status = review["change_status"]
        counts[status] = counts.get(status, 0) + 1
    print(json.dumps({
        "source_rows": len(source_rows),
        "policy_rows": len(policies),
        "yellow_rows": sum(row["default_policy"] == "YELLOW" for row in policies),
        "review_required_rows": sum(row["review_required"] == "1" for row in reviews),
        "changes": counts,
        "policy_output": str(policy_path.resolve()),
        "review_output": str(review_path.resolve()),
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
