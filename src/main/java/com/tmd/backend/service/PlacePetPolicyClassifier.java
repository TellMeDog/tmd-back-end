package com.tmd.backend.service;

import com.tmd.backend.ai.AccessScope;
import com.tmd.backend.ai.PetPolicyAnalysis;
import com.tmd.backend.ai.WeightLimitType;
import com.tmd.backend.common.PetInfoStatus;
import com.tmd.backend.domain.place.PlacePetInfo;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PlacePetPolicyClassifier {
    private static final Pattern EXPLICIT_ALL_BREEDS = Pattern.compile("전\\s*견종|모든\\s*견종|전체\\s*견종");
    private static final Pattern LIMITED_BREEDS = Pattern.compile(
        "맹견\\s*(?:은|의\\s*경우)?\\s*(?:제외|불가|금지)|일부\\s*견종|소형견|중소형견|중형견|대형견|안내견|특정\\s*견종|견종에\\s*따라|견종별"
    );
    private static final Pattern DANGEROUS_EXCLUDED = Pattern.compile(
        "맹견\\s*(?:은|의\\s*경우)?\\s*(?:제외|X)|맹견.{0,18}(?:입장|출입|동반|입실)\\s*(?:불가|제한|금지)|(?:입장|출입|동반|입실)\\s*제한\\s*반려견.{0,80}맹견"
    );
    private static final Pattern DANGEROUS_MUZZLE = Pattern.compile("맹견.{0,35}입마개|입마개.{0,35}맹견");
    private static final Pattern GENERAL_DENIAL = Pattern.compile(
        "^(?:반려동물|반려견|애완동물)?\\s*(?:동반|입장|출입)?\\s*(?:불가|금지)$|(?:반려동물|반려견|애완동물)\\s*(?:동반|입장|출입)\\s*(?:불가|금지)"
    );
    private static final Pattern PARTIAL_ACCESS = Pattern.compile(
        "실내.{0,15}(?:동반|입장|출입)?\\s*(?:불가|제한)|내부.{0,15}(?:동반|입장|출입)?\\s*(?:불가|제한)|" +
            "외부.{0,8}(?:만|한정).{0,8}(?:가능|이용)|야외.{0,8}(?:만|한정).{0,8}(?:가능|이용)|" +
            "일부\\s*(?:구역|시설)|지정.{0,8}(?:구역|장소)|산책로만|테라스만|" +
            "(?:전시관|체험시설|캠핑장|객실|카라반|방갈로|수영장|해수욕장|레스토랑|식당|카페|매장|화장실|휴게시설|부대시설|식음업장|선실).{0,18}(?:동반|입장|입실|출입|이용)?\\s*(?:불가|제한|금지)|입수\\s*(?:불가|제한|금지)"
    );
    private static final Pattern ASSISTANCE_ONLY = Pattern.compile(
        "^(?:(?:시각\\s*장애인|맹인|장애우)\\s*)?(?:안내견|보조견)(?:만)?(?:\\s*(?:가능|가))?$|^불가\\s*\\(\\s*보조견만\\s*가능\\s*\\)$|^(?:장애우\\s*)?안내견만\\s*이용가능$"
    );
    private static final Pattern VARIABLE_ACCESS = Pattern.compile("점포마다\\s*다름|매장.{0,15}(?:상이|정책)|개별\\s*매장");
    private static final Pattern AFFIRMATIVE_ACCESS = Pattern.compile(
        "동반\\s*(?:입장|출입)?\\s*가능|입장\\s*가능|출입\\s*가능|이용객에\\s*한하여\\s*허용|^가능\\s*\\(|반려견\\s*\\([^)]*kg[^)]*\\)|(?:소형견|중소형견|중형견|대형견)(?:만|\\s|.*가능)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WEIGHT = Pattern.compile("(?<!\\d)(\\d+(?:\\.\\d+)?)\\s*(?:kg|㎏|킬로그램|킬로)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNT = Pattern.compile("(?:최대\\s*)?(\\d+)\\s*마리(?:까지|만)?\\s*(?:동반|입장|출입|가능|허용)");
    private static final Map<String, Pattern> NON_COMPARABLE = Map.ofEntries(
        Map.entry("vaccination", Pattern.compile("예방\\s*접종|백신|접종증")),
        Map.entry("stroller", Pattern.compile("유모차")),
        Map.entry("manner_belt", Pattern.compile("매너\\s*벨트|기저귀")),
        Map.entry("waste_bag", Pattern.compile("배변\\s*봉투|배설물|배변\\s*처리")),
        Map.entry("registration", Pattern.compile("동물\\s*등록|등록증")),
        Map.entry("health_or_behavior", Pattern.compile("질병|발정|공격성|사나운|사납|짖음|중성화")),
        Map.entry("age_or_sex", Pattern.compile("\\d+\\s*(?:개월|세)\\s*(?:이상|이하|미만)|수컷|암컷")),
        Map.entry("guardian_condition", Pattern.compile("보호자.{0,15}(?:동반|책임|성인|나이)|\\d+세\\s*이상.{0,10}보호자")),
        Map.entry("site_variability", Pattern.compile("현지\\s*사정|운영\\s*정책.{0,10}변동|혼잡도|상황에\\s*따라|현장.{0,8}판단")),
        Map.entry("breed_not_representable", Pattern.compile("일부\\s*견종|소형견|중소형견|중형견|대형견|안내견|특정\\s*견종|견종별|견종에\\s*따라"))
    );

    public Classification classify(PlacePetInfo info) {
        if (info.getStatus() == PetInfoStatus.NO_DATA) {
            return result(info, AccessScope.UNKNOWN, null, null, null, null, WeightLimitType.UNKNOWN,
                null, null, null, null, null, null, List.of(), List.of("NO_DATA=>UNKNOWN/GREY"));
        }

        String type = clean(info.getAcmpyTypeCd());
        String capacity = clean(info.getAcmpyPsblCpam());
        String need = clean(info.getAcmpyNeedMtr());
        String decision = join(capacity, need, info.getRelaAcdntRiskMtr(), info.getEtcAcmpyInfo());
        String combined = join(type, decision, info.getRelaPosesFclty(), info.getRelaFrnshPrdlst(), info.getRelaPurcPrdlst(), info.getRelaRntlPrdlst());
        LinkedHashSet<String> review = new LinkedHashSet<>();
        LinkedHashSet<String> yellow = new LinkedHashSet<>();

        AccessScope scope;
        if (type.matches(".*일부\\s*구역.*")) scope = AccessScope.PARTIAL;
        else if (type.matches(".*전\\s*구역.*")) scope = AccessScope.ALL;
        else if (ASSISTANCE_ONLY.matcher(capacity).matches() || ASSISTANCE_ONLY.matcher(clean(info.getEtcAcmpyInfo())).matches()) scope = AccessScope.NONE;
        else if (VARIABLE_ACCESS.matcher(decision).find()) { scope = AccessScope.PARTIAL; yellow.add("variable_access"); }
        else if (AFFIRMATIVE_ACCESS.matcher(decision).find() || capacity.matches("(?:소형견|중소형견|중형견|대형견|반려견)")) scope = AccessScope.ALL;
        else if (GENERAL_DENIAL.matcher(decision).find()) scope = AccessScope.NONE;
        else scope = AccessScope.UNKNOWN;

        if (PARTIAL_ACCESS.matcher(decision).find()) {
            if (scope == AccessScope.ALL) review.add("scope_conflict");
            scope = AccessScope.PARTIAL;
        }
        if (GENERAL_DENIAL.matcher(capacity).matches()) {
            if (scope == AccessScope.ALL || scope == AccessScope.PARTIAL) review.add("scope_conflict");
            scope = AccessScope.NONE;
        }
        if (scope == AccessScope.PARTIAL) yellow.add("partial_access");

        Boolean allBreeds = null;
        if (LIMITED_BREEDS.matcher(decision).find() || WEIGHT.matcher(decision).find()) {
            allBreeds = false;
        } else if (EXPLICIT_ALL_BREEDS.matcher(decision).find()) {
            allBreeds = true;
        }
        Boolean dangerousAllowed = null;
        String dangerousCondition = null;
        if (DANGEROUS_EXCLUDED.matcher(decision).find()) dangerousAllowed = false;
        else if (DANGEROUS_MUZZLE.matcher(decision).find()) { dangerousAllowed = true; dangerousCondition = "MUZZLE"; }
        else if (Boolean.TRUE.equals(allBreeds)) dangerousAllowed = true;

        WeightResult weight = extractWeight(decision, review, yellow);
        Boolean leash = find("목줄|리드\\s*줄", decision) ? true : null;
        Boolean muzzle = need.contains("입마개") || Arrays.stream(sentences(decision)).anyMatch(s -> s.contains("입마개") && !s.contains("맹견")) ? true : null;
        Boolean kennel = find("이동장|켄넬|케이지|캐리어", decision) ? true : null;
        Boolean inquiry = find("사전\\s*문의|전화\\s*문의|문의\\s*(?:필수|후|바람|요망)|확인\\s*후", decision) ? true : null;
        if (Boolean.TRUE.equals(inquiry)) yellow.add("advance_inquiry");

        SortedSet<Integer> counts = new TreeSet<>();
        Matcher countMatcher = COUNT.matcher(decision);
        while (countMatcher.find()) counts.add(Integer.parseInt(countMatcher.group(1)));
        Integer maxCount = counts.isEmpty() ? null : counts.first();
        if (maxCount != null) yellow.add("group_pet_count");
        if (counts.size() > 1) review.add("multiple_pet_counts");
        NON_COMPARABLE.forEach((name, pattern) -> { if (pattern.matcher(decision).find()) yellow.add(name); });
        if (scope == AccessScope.UNKNOWN && !combined.isBlank()) review.add("scope_unknown_with_source_text");
        if (!review.isEmpty()) yellow.add("manual_review");
        if (scope == AccessScope.NONE) yellow.clear();

        return result(info, scope, allBreeds, dangerousAllowed, dangerousCondition, weight.value(), weight.type(),
            leash, muzzle, kennel, inquiry, maxCount, yellow.isEmpty() ? null : "YELLOW", List.copyOf(review), List.copyOf(yellow));
    }

    private WeightResult extractWeight(String text, Set<String> review, Set<String> yellow) {
        List<WeightCandidate> candidates = new ArrayList<>();
        for (String sentence : sentences(text)) {
            Matcher matcher = WEIGHT.matcher(sentence);
            while (matcher.find()) {
                double value = Double.parseDouble(matcher.group(1));
                boolean admission = find("동반|입장|입실|출입|가능|허용|불가|제한|전용|객실당", sentence);
                if (find("입마개|이동장|켄넬|목줄", sentence) && !admission) continue;
                String suffix = sentence.substring(matcher.end());
                WeightLimitType type = suffix.matches("^\\s*미만.*") ? WeightLimitType.LESS_THAN
                    : suffix.matches("^\\s*(?:이하|이내).*|^\\s*초과.*(?:불가|제한).*") ? WeightLimitType.LESS_THAN_OR_EQUAL
                    : suffix.matches("^\\s*이상.*(?:불가|제한).*") ? WeightLimitType.LESS_THAN
                    : WeightLimitType.UNKNOWN;
                if (admission) candidates.add(new WeightCandidate(value, type));
            }
        }
        if (candidates.isEmpty()) return new WeightResult(null, WeightLimitType.UNKNOWN);
        double minimum = candidates.stream().mapToDouble(WeightCandidate::value).min().orElseThrow();
        Set<WeightLimitType> types = new HashSet<>();
        candidates.stream().filter(c -> c.value() == minimum && c.type() != WeightLimitType.UNKNOWN).forEach(c -> types.add(c.type()));
        WeightLimitType type = types.size() == 1 ? types.iterator().next() : types.size() > 1 ? WeightLimitType.LESS_THAN : WeightLimitType.UNKNOWN;
        if (types.size() != 1) { review.add("ambiguous_weight_boundary"); yellow.add("ambiguous_weight_boundary"); }
        if (candidates.stream().map(WeightCandidate::value).distinct().count() > 1) { review.add("multiple_weight_values"); yellow.add("multiple_weight_values"); }
        return new WeightResult(minimum, type);
    }

    private Classification result(PlacePetInfo info, AccessScope scope, Boolean allBreeds, Boolean dangerous,
                                  String condition, Double weight, WeightLimitType weightType, Boolean leash,
                                  Boolean muzzle, Boolean kennel, Boolean inquiry, Integer count, String defaultPolicy,
                                  List<String> review, List<String> yellow) {
        return new Classification(new PetPolicyAnalysis(info.getPlace().getId(), scope, allBreeds, dangerous, condition,
            weight, weightType, leash, muzzle, kennel, inquiry, count, defaultPolicy), review, yellow);
    }

    private static boolean find(String regex, String text) { return Pattern.compile(regex).matcher(text).find(); }
    private static String clean(String value) { return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC).trim(); }
    private static String join(String... values) { return Arrays.stream(values).map(PlacePetPolicyClassifier::clean).filter(v -> !v.isBlank()).reduce((a, b) -> a + "\n" + b).orElse(""); }
    private static String[] sentences(String text) { return text.split("[\\n\\r]|(?<=[.!?])\\s*|\\s+-\\s+|\\s*/\\s*|※"); }

    public record Classification(PetPolicyAnalysis analysis, List<String> reviewReasons, List<String> yellowReasons) {
        public boolean requiresReview() { return !reviewReasons.isEmpty(); }
    }
    private record WeightCandidate(double value, WeightLimitType type) {}
    private record WeightResult(Double value, WeightLimitType type) {}
}
