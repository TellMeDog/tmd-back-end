import argparse
import csv
from pathlib import Path


POLICY_COLUMNS = [
    "access_scope", "all_breeds_allowed", "dangerous_breed_allowed",
    "dangerous_breed_allowed_condition", "max_weight_kg", "weight_limit_type",
    "leash_required", "muzzle_required", "kennel_required",
    "advance_inquiry_required", "max_pet_count", "default_policy",
]
NUMERIC_COLUMNS = {
    "all_breeds_allowed", "dangerous_breed_allowed", "max_weight_kg", "leash_required",
    "muzzle_required", "kennel_required", "advance_inquiry_required", "max_pet_count",
}


def sql_value(column, value):
    if value is None or value == "":
        return "NULL"
    if column in NUMERIC_COLUMNS:
        if str(value).lower() == "true":
            return "1"
        if str(value).lower() == "false":
            return "0"
        return str(value)
    return f"CONVERT(0x{str(value).encode('utf-8').hex()} USING utf8mb4)"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--reviewed-csv", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    with args.reviewed_csv.open(encoding="utf-8-sig", newline="") as source:
        rows = list(csv.DictReader(source))
    review_ids = [row.get("review_id", "") for row in rows]
    if not rows or "" in review_ids or len(review_ids) != len(set(review_ids)):
        raise ValueError("검토 CSV에는 고유한 review_id가 필요합니다.")

    statements = ["SET NAMES utf8mb4;", "START TRANSACTION;"]
    for row in rows:
        resolution = row.get("resolution", "").strip().upper()
        if resolution not in {"APPLY", "KEEP"}:
            raise ValueError(f"review_id={row['review_id']}: resolution은 APPLY 또는 KEEP이어야 합니다.")
        review_id = int(row["review_id"])
        if resolution == "APPLY":
            assignments = ", ".join(
                f"p.{column} = {sql_value(column, row.get(column))}" for column in POLICY_COLUMNS
            )
            statements.append(
                f"UPDATE place_pet_policy p JOIN place_pet_policy_review r ON r.place_id=p.place_id "
                f"SET {assignments}, p.review_pending=0 WHERE r.id={review_id} AND r.status='PENDING';"
            )
        else:
            statements.append(
                f"UPDATE place_pet_policy p JOIN place_pet_policy_review r ON r.place_id=p.place_id "
                f"SET p.review_pending=0 WHERE r.id={review_id} AND r.status='PENDING';"
            )
        note = row.get("review_note", "")
        note_sql = f"CONVERT(0x{note.encode('utf-8').hex()} USING utf8mb4)" if note else "NULL"
        statements.append(
            f"UPDATE place_pet_policy_review SET status='RESOLVED', resolved_at=NOW(6), "
            f"review_reasons=CONCAT(review_reasons, IF({note_sql} IS NULL, '', CONCAT(' | note: ', {note_sql}))) "
            f"WHERE id={review_id} AND status='PENDING';"
        )
    statements.extend(["COMMIT;", "SELECT COUNT(*) AS pending_reviews FROM place_pet_policy_review WHERE status='PENDING';"])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(statements) + "\n", encoding="utf-8", newline="\n")
    print(f"Resolution SQL generated: {args.output} ({len(rows)} rows)")


if __name__ == "__main__":
    main()
