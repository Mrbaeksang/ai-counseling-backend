package com.aicounseling.app.domain.session.entity

/**
 * 심리 상담의 진행 단계를 정의하는 Enum 클래스
 * 전통적인 상담 모델과 현대적인 접근을 결합하여 설계됨
 * 학문적 근거에 기반한 체계적인 5단계 상담 과정
 */
enum class CounselingPhase(
    val stageTitle: String,
    val koreanName: String,
    val objective: String,
    val checklist: List<String>,
    val tone: String,
    val transitionHint: String,
) {
    /**
     * 1단계: 관계 형성 (Engagement)
     * 내담자와 신뢰 관계(라포)를 구축하고 편안한 분위기 조성
     * 첫 인사, 상담 목표 설정, 참여 동기 부여
     */
    ENGAGEMENT(
        stageTitle = "STAGE 1: ENGAGEMENT",
        koreanName = "관계 형성",
        objective = "신뢰를 쌓고 주 호소 문제와 상담 동기를 파악합니다.",
        checklist =
            listOf(
                "따뜻한 인사와 편안한 분위기를 조성하세요.",
                "개방형 질문으로 내담자의 생각과 감정을 끌어내세요.",
                "상담의 목적과 기대를 안전하게 확인하세요.",
            ),
        tone = "따뜻하고 안정적인 톤, 과한 공손어를 피한 자연스러운 존댓말",
        transitionHint = "내담자가 구체적인 상황이나 최근 경험을 이야기하기 시작하면 EXPLORATION 단계로 이동합니다.",
    ),

    /**
     * 2단계: 문제 탐색 (Exploration)
     * 내담자의 문제를 다각적으로 탐색하고 구체적인 경험과 감정 파악
     * 문제 상황, 감정, 배경을 깊이 있게 이해
     */
    EXPLORATION(
        stageTitle = "STAGE 2: EXPLORATION",
        koreanName = "문제 탐색",
        objective = "문제의 구체적인 양상과 반복되는 패턴을 이해합니다.",
        checklist =
            listOf(
                "문제의 시작 시점과 촉발 요인을 확인하세요.",
                "최근 사례를 중심으로 감정-사고-행동의 연결고리를 탐색하세요.",
                "과거에 시도했던 대처 방식과 그 결과를 함께 살펴보세요.",
            ),
        tone = "차분하고 호기심 어린 톤, 내담자의 표현을 재진술하며 이해를 보여주기",
        transitionHint = "핵심 사고나 반복 패턴이 드러나면 INSIGHT 단계로 전환하여 의미를 재구성하세요.",
    ),

    /**
     * 3단계: 통찰 유도 (Insight)
     * 내담자가 자신의 패턴을 발견하고 새로운 관점 획득
     * 깊은 성찰을 통한 자기 이해와 깨달음 촉진
     */
    INSIGHT(
        stageTitle = "STAGE 3: INSIGHT",
        koreanName = "통찰 유도",
        objective = "자동적 사고를 인식하고 대안적 관점을 개발합니다.",
        checklist =
            listOf(
                "상황에서 떠오른 자동적 사고를 명확히 언어화하세요.",
                "증거 탐색, 대안 질문 등 소크라테스식 질문으로 사고를 검토하세요.",
                "보다 현실적이고 도움이 되는 사고를 함께 정리하세요.",
            ),
        tone = "공감적이면서도 명료한 톤, 내담자의 통찰을 격려하고 확장시키기",
        transitionHint = "새로운 관점과 변화 의지가 확인되면 ACTION 단계로 넘어가 실천 계획을 세웁니다.",
    ),

    /**
     * 4단계: 행동 계획 (Action)
     * 문제 해결을 위한 구체적이고 실천 가능한 계획 수립
     * 작은 변화부터 시작하여 점진적 개선 도모
     */
    ACTION(
        stageTitle = "STAGE 4: ACTION",
        koreanName = "행동 계획",
        objective = "실행 가능한 계획을 세우고 실행 전략을 마련합니다.",
        checklist =
            listOf(
                "SMART 기준에 맞춘 구체적 목표를 함께 설정하세요.",
                "실행 시 예상되는 장애물을 점검하고 대처 전략을 마련하세요.",
                "행동을 촉진할 수 있는 자기 격려 문장이나 도구를 제안하세요.",
            ),
        tone = "격려하면서도 현실적인 톤, 실천을 돕는 구체적 제안 포함",
        transitionHint = "실행 계획이 정리되고 실행 의지가 충분하면 CLOSING 단계로 전환하여 상담을 마무리합니다.",
    ),

    /**
     * 5단계: 마무리 (Closing)
     * 상담 내용 정리와 긍정적 마무리
     * 오늘 대화의 성과 확인과 다음 만남 기대
     */
    CLOSING(
        stageTitle = "STAGE 5: CLOSING",
        koreanName = "마무리",
        objective = "오늘의 성과를 정리하고 다음을 준비할 수 있도록 돕습니다.",
        checklist =
            listOf(
                "오늘 대화에서 얻은 통찰 2~3가지를 정리하세요.",
                "내담자의 강점과 사용 가능한 자원을 구체적으로 칭찬하세요.",
                "향후 실천할 행동이나 다음 만남에 대한 기대를 확인하세요.",
            ),
        tone = "따뜻하고 긍정적인 톤, 감사와 응원이 담긴 마무리",
        transitionHint = "필요하다면 다음 상담 일정을 조율하거나 추가 지원 자원을 안내하세요.",
    ),
    ;

    val displayName: String get() = "$stageTitle ($koreanName)"
    val description: String get() = objective
}
