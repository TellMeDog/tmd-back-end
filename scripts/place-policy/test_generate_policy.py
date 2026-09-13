import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("generate_policy.py")
SPEC = importlib.util.spec_from_file_location("generate_policy", MODULE_PATH)
POLICY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(POLICY)


def source_row(place_id="1", status="SUCCESS", **values):
    row = {column: "" for column in POLICY.RAW_COLUMNS}
    row.update({"place_id": place_id, "content_id": f"content-{place_id}", "status": status})
    row.update(values)
    return row


class GeneratePolicyTest(unittest.TestCase):
    def test_no_data_is_unknown_without_yellow(self):
        result, _, reasons, yellow = POLICY.classify(source_row(status="NO_DATA"))
        self.assertEqual("UNKNOWN", result["access_scope"])
        self.assertEqual("", result["default_policy"])
        self.assertEqual([], reasons)
        self.assertEqual([], yellow)

    def test_dangerous_dog_muzzle_is_not_general_muzzle_requirement(self):
        result, *_ = POLICY.classify(source_row(
            acmpy_type_cd="전구역 동반가능",
            acmpy_psbl_cpam="전 견종 동반 가능",
            etc_acmpy_info="맹견의 경우, 입마개 착용 필수",
        ))
        self.assertEqual("1", result["dangerous_breed_allowed"])
        self.assertEqual("MUZZLE", result["dangerous_breed_allowed_condition"])
        self.assertEqual("", result["muzzle_required"])

    def test_equipment_weight_is_not_admission_weight(self):
        result, rules, *_ = POLICY.classify(source_row(
            acmpy_type_cd="전구역 동반가능",
            etc_acmpy_info="10kg 이상 대형견은 입마개 착용 필수",
        ))
        self.assertEqual("", result["max_weight_kg"])
        self.assertIn("ignored_equipment_weight=10", rules)

    def test_unavailable_pet_data_condition_sets_yellow(self):
        result, *_ = POLICY.classify(source_row(
            acmpy_type_cd="전구역 동반가능",
            etc_acmpy_info="예방접종 확인서와 배변봉투 지참 필수",
        ))
        self.assertEqual("YELLOW", result["default_policy"])

    def test_incremental_change_preserves_policy_and_requires_review(self):
        old_source = source_row(acmpy_type_cd="전구역 동반가능")
        old_policy, *_ = POLICY.classify(old_source)
        old_policy["default_policy"] = "YELLOW"
        old_review = {
            **old_source,
            "source_status": old_source["status"],
            "source_fingerprint": POLICY.source_fingerprint(old_source),
        }
        changed_source = source_row(
            acmpy_type_cd="일부구역 동반가능",
            etc_acmpy_info="실내 동반 불가",
        )

        policies, reviews = POLICY.merge_rows(
            [changed_source], {"1": old_policy}, {"1": old_review}
        )

        self.assertEqual("YELLOW", policies[0]["default_policy"])
        self.assertEqual("SOURCE_CHANGED", reviews[0]["change_status"])
        self.assertEqual("1", reviews[0]["review_required"])
        self.assertIn("source_changed", reviews[0]["review_reasons"])
        self.assertEqual("PARTIAL", reviews[0]["suggested_access_scope"])

    def test_missing_source_preserves_existing_policy(self):
        old_policy, *_ = POLICY.classify(source_row())
        policies, reviews = POLICY.merge_rows([], {"1": old_policy}, {})

        self.assertEqual(old_policy, policies[0])
        self.assertEqual("SOURCE_MISSING", reviews[0]["change_status"])
        self.assertEqual("1", reviews[0]["review_required"])


if __name__ == "__main__":
    unittest.main()
