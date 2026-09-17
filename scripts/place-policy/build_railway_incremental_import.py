import argparse
import csv
import json
from pathlib import Path


INFO_COLUMNS = [
    "acmpy_type_cd", "acmpy_psbl_cpam", "acmpy_need_mtr", "rela_acdnt_risk_mtr",
    "rela_poses_fclty", "rela_frnsh_prdlst", "rela_purc_prdlst", "rela_rntl_prdlst",
    "etc_acmpy_info", "status",
]
POLICY_COLUMNS = [
    "access_scope", "all_breeds_allowed", "dangerous_breed_allowed",
    "dangerous_breed_allowed_condition", "max_weight_kg", "weight_limit_type",
    "leash_required", "muzzle_required", "kennel_required",
    "advance_inquiry_required", "max_pet_count", "default_policy",
]
NUMERIC_POLICY_COLUMNS = {
    "all_breeds_allowed", "dangerous_breed_allowed", "max_weight_kg", "leash_required",
    "muzzle_required", "kennel_required", "advance_inquiry_required", "max_pet_count",
}


def sql_value(value):
    if value is None or value == "":
        return "NULL"
    encoded = str(value).encode("utf-8").hex()
    return f"CONVERT(0x{encoded} USING utf8mb4)"


def policy_sql_value(column, value):
    if value is None or value == "":
        return "NULL"
    if column in NUMERIC_POLICY_COLUMNS:
        return str(value)
    return sql_value(value)


def load_ids(path):
    return {line.strip() for line in path.read_text(encoding="utf-8-sig").splitlines() if line.strip()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--policy", type=Path, required=True)
    parser.add_argument("--remote-info-ids", type=Path, required=True)
    parser.add_argument("--remote-policy-ids", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    source_rows = [json.loads(line) for line in args.source.read_text(encoding="utf-8").splitlines() if line]
    source_by_place_id = {str(row["place_id"]): row for row in source_rows}
    remote_info_ids = load_ids(args.remote_info_ids)
    remote_policy_ids = load_ids(args.remote_policy_ids)

    with args.policy.open(encoding="utf-8-sig", newline="") as policy_file:
        policy_rows = list(csv.DictReader(policy_file))

    statements = ["SET NAMES utf8mb4;", "START TRANSACTION;"]
    info_count = 0
    for row in source_rows:
        content_id = str(row["content_id"])
        if content_id in remote_info_ids:
            continue
        values = ", ".join(sql_value(row.get(column)) for column in INFO_COLUMNS)
        statements.append(
            "INSERT INTO place_pet_info "
            "(place_id, acmpy_type_cd, acmpy_psbl_cpam, acmpy_need_mtr, rela_acdnt_risk_mtr, "
            "rela_poses_fclty, rela_frnsh_prdlst, rela_purc_prdlst, rela_rntl_prdlst, etc_acmpy_info, status) "
            f"SELECT p.id, {values} FROM place p WHERE p.content_id = {sql_value(content_id)} "
            "AND NOT EXISTS (SELECT 1 FROM place_pet_info x WHERE x.place_id = p.id);"
        )
        info_count += 1

    policy_count = 0
    for row in policy_rows:
        source = source_by_place_id[str(row["place_id"])]
        content_id = str(source["content_id"])
        if content_id in remote_policy_ids:
            continue
        values = ", ".join(policy_sql_value(column, row.get(column)) for column in POLICY_COLUMNS)
        statements.append(
            "INSERT INTO place_pet_policy "
            "(place_id, access_scope, all_breeds_allowed, dangerous_breed_allowed, dangerous_breed_allowed_condition, "
            "max_weight_kg, weight_limit_type, leash_required, muzzle_required, kennel_required, "
            "advance_inquiry_required, max_pet_count, default_policy) "
            f"SELECT p.id, {values} FROM place p WHERE p.content_id = {sql_value(content_id)} "
            "AND NOT EXISTS (SELECT 1 FROM place_pet_policy x WHERE x.place_id = p.id);"
        )
        policy_count += 1

    statements.extend([
        "COMMIT;",
        "SELECT 'place' AS table_name, COUNT(*) AS row_count FROM place "
        "UNION ALL SELECT 'place_pet_info', COUNT(*) FROM place_pet_info "
        "UNION ALL SELECT 'place_pet_policy', COUNT(*) FROM place_pet_policy;",
        "SELECT COUNT(*) AS orphan_info FROM place_pet_info x LEFT JOIN place p ON p.id=x.place_id WHERE p.id IS NULL;",
        "SELECT COUNT(*) AS orphan_policy FROM place_pet_policy x LEFT JOIN place p ON p.id=x.place_id WHERE p.id IS NULL;",
    ])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(statements) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps({"info_inserts": info_count, "policy_inserts": policy_count, "output": str(args.output)}))


if __name__ == "__main__":
    main()
