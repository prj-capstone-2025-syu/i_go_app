# IGO - AI 지각 방지 솔루션 📱

> 웹 기반 AI 지각 방지 솔루션을 Android WebView 앱으로 포팅한 버전입니다.


## 🚀 프로젝트 개요

IGO는 AI를 활용한 지각 방지 솔루션으로, 기존 웹 서비스를 Android 앱으로 포팅하여 사용자에게 더 나은 모바일 경험을 제공합니다.

### 주요 특징
- WebView 기반 하이브리드 앱
- 구글 소셜 로그인 지원
- Firebase 푸시 알림 기능

## 🔧 개발 환경

### 필수 요구사항
- Android Studio Arctic Fox+
- JDK 11+
- Android SDK (API 24-36)
- Firebase 프로젝트 설정

### Firebase 설정
1. Firebase Console에서 프로젝트 생성
2. Android 앱 추가 및 `google-services.json` 다운로드
3. `app/` 디렉터리에 파일 배치

## 📱 지원 환경
- **최소 지원**: Android 7.0 (API 24)
- **권장 환경**: Android 10+ (API 29+)
- **아키텍처**: Universal APK (모든 기기)

## 🏃‍♂️ 프로젝트 실행

### 1. Android 프로젝트 열기
1. Android Studio 실행
2. `Open an Existing Project` 선택
3. `IGO` 프로젝트 폴더 선택
4. Gradle Sync 완료 대기

### 2. 앱 실행
1. **에뮬레이터 설정**: API 24 이상의 Android 에뮬레이터 생성
2. **실제 기기 연결**: USB 디버깅 활성화 후 연결
3. **실행**: `Run` → `Run 'app'` 클릭 또는 `Shift + F10`

## 📦 APK 빌드 방법

### 방법 1: Android Studio GUI 사용 (권장)

#### Debug APK 빌드 (개발/테스트용)
1. `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)` 클릭
2. 빌드 완료 후 `locate` 링크 클릭하여 APK 위치 확인
3. 생성된 파일: `IGO_AI_v1.1-debug_debug.apk`

#### Release APK 빌드 (배포용)
1. `Build` → `Generate Signed Bundle / APK` 선택
2. `APK` 선택 후 `Next`
3. 키스토어 생성 또는 기존 키스토어 선택
4. 빌드 타입을 `release` 선택
5. 생성된 파일: `IGO_AI_v1.1_release.apk`

### 방법 2: Gradle 명령어 사용

#### Debug 빌드
```bash
./gradlew assembleDebug
```

#### Release 빌드
```bash
./gradlew assembleRelease
```

### APK 파일 위치
```
app/build/outputs/apk/
├── debug/
│   └── IGO_AI_v1.1-debug_debug.apk
└── release/
    └── IGO_AI_v1.1_release.apk
```


## 📁 파일 구조

```
IGO/
├── app/                          # Android 앱 모듈
│   ├── build.gradle.kts         # 앱 빌드 설정
│   ├── google-services.json     # Firebase 설정 (Git 제외)
│   └── src/main/
│       ├── AndroidManifest.xml  # 앱 권한 및 설정
│       ├── java/com/example/igo_ai/
│       │   ├── MainActivity.java      # 메인 액티비티
│       │   ├── Firebaseinit.java     # Firebase 초기화
│       │   └── MyFirebaseMessagingService.java  # FCM 서비스
│       └── res/
│           ├── layout/           # UI 레이아웃
│           ├── values/           # 문자열, 색상 등
│           └── mipmap/           # 앱 아이콘
├── src/                         # Spring Boot 백엔드
│   └── main/
│       ├── java/                # Java 소스코드
│       └── resources/           # 설정 파일 및 정적 리소스
├── build.gradle                 # 프로젝트 전체 빌드 설정
├── settings.gradle              # 모듈 설정
├── .gitignore                   # Git 제외 파일 목록
└── README.md                    # 이 파일
```
