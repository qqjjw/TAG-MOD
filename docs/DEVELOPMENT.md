# 개발 및 Git 관리

## 작업 원칙

- 코드 검사·컴파일·자동 테스트를 실행하기 전에 사용자에게 목적과 범위를 설명하고 승인을 받습니다.
- 일반 빌드에는 테스트 옵션을 붙이지 않습니다. 작은 변경에 전체 자동 검사를 반복하지 않습니다.
- 실제 게임 실행과 LAN 플레이 확인은 사용자가 담당합니다.
- 설정으로 해결할 수 있는 확률·수치 조정은 설정 파일을 사용합니다. 기존 게임 설정은 자동으로 덮어쓰지 않습니다.

## 소스 구성

| 위치 | 역할 |
|---|---|
| `src/main/java/dev/tagmod/TagMod.java` | 초기화, 서버 이벤트, 소지 장비 처리, 테스트 명령 |
| `Config.java` | 설정 기본값·읽기·잘못된 항목의 대체값 |
| `Monsters.java` | 몹 대상, 강화·무장·대형화, 저장 태그와 변환 계승 |
| `Gear.java`, `GearData.java`, `Option.java`, `Rolls.java` | 장비 후보·추첨·저장·능력치·한국어 표시 |
| `Effects.java`, `Combat.java` | 장착 효과의 소유권, 외부 포션 공존, 투사체·흡혈 |
| `Preview.java` | 수리·합성 결과의 고정 추첨 |
| `src/main/java/dev/tagmod/mixin/` | 아이템 이름·툴팁, 인벤토리, 수리·합성, 전투 등에 연결 |
| `src/main/resources/` | Fabric 메타데이터, Mixin 등록 |
| `src/test/java/dev/tagmod/` | 선택적 자동 검사와 특정 오류의 회귀 검사 |
| `config/tagmod.json` | Git으로 관리하는 배포용 기본 설정 |
| `scripts/` | 빌드·선택적 검사·패키징 |

파일명만 적은 항목은 `src/main/java/dev/tagmod/` 아래에 있습니다.

## 빌드 준비

Windows PowerShell과 PATH에 등록된 JDK 25의 `java`, `javac`, `jar`가 필요합니다. 설치된 공식 Minecraft 26.3 게임 JAR, 라이브러리, Fabric Loader 0.19.5 프로필, 26.3용 Fabric API를 사용합니다. 빌드가 의존성을 다운로드하거나 게임을 실행하지 않습니다.

현재 프로젝트는 Gradle/Loom 대신 `scripts/build.ps1`의 직접 컴파일 방식을 사용합니다. 26.3의 공개 클래스 이름을 사용하므로 재매핑 단계는 없습니다. Minecraft 버전을 바꾸려면 Java 코드와 빌드 스크립트의 게임·프로필·API 경로를 함께 수정해야 합니다.

## 컴파일과 설치 파일 생성

아래 명령은 **프로젝트 루트**에서 필요한 실행 승인을 받은 뒤 사용합니다.

```powershell
# 컴파일과 JAR 생성. 자동 테스트를 실행하지 않음.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build.ps1

# 기존 빌드 JAR, 기본 설정, 문서를 모아 ZIP 생성. 컴파일·테스트하지 않음.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\package.ps1
```

빌드 출력은 `build/libs/tagmod-<버전>.jar`, 패키지는 `dist/TagMod-<버전>.zip`과 같은 이름의 디렉터리입니다. 버전은 `fabric.mod.json`에서 읽습니다. 패키징 전에 해당 버전의 JAR가 빌드돼 있어야 합니다.

설치된 게임 파일이 기본 `.minecraft` 폴더와 다르면 빌드 명령에 `-MinecraftDirectory '경로'`를 지정합니다. 이 경로에는 게임 JAR·라이브러리·Fabric 프로필·API가 있어야 하므로 게임 실행에만 쓰는 별도 프로필 폴더와 구분하세요.

게임에 적용할 때는 기존 TagMod JAR를 빼고 새 JAR를 넣습니다. 프로젝트의 설정 파일 변경은 게임 디렉터리로 직접 복사해야 반영됩니다.

## 변경할 때 주의할 동작

- 장비 데이터는 `custom_data`의 `tagmod` 항목에 저장합니다. 저장 형식을 바꿀 때는 `GearData.VERSION`과 기존 장비의 호환 처리를 함께 고려합니다.
- 다른 데이터·인챈트·사용자 이름을 보존하고 재추첨 시 기존 모드 수정자를 제거한 뒤 새 수정자를 적용합니다.
- 클라이언트에서는 새 옵션을 추첨하지 않습니다. 호스트가 계산한 아이템 데이터와 기본 속성 동기화를 사용합니다.
- 몹은 처리·특성·크기를 엔티티 태그와 속성으로 보존합니다. 로드·변환 시 재추첨하거나 크기를 누적하지 않습니다.
- 효과 적용 콜백은 엔티티 태그를 변경할 수 있습니다. 콜백을 호출하는 루프는 살아 있는 태그 집합 대신 스냅샷을 순회해야 합니다. 0.1.1에서 수정한 충돌의 원인입니다.
- 장착 효과를 제거할 때 모드가 소유한 효과만 제거합니다. 플레이어가 마신 포션·비콘 효과를 지우지 않도록 유지합니다.
- 새 설정은 코드 기본값, `config/tagmod.json`, 설정 문서를 함께 갱신합니다. 버전 변경 시 메타데이터·초기화 로그·README의 버전 표기도 맞춥니다.

## 선택적 검사

**각 명령을 실행하기 전에 별도 승인을 받습니다.** 기본 빌드와 분리된 명령입니다.

| 목적 | 명령 |
|---|---|
| 태그 순회 충돌 회귀 시나리오 1건 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build.ps1 -Regression` |
| 전체 자동 검사 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build.ps1 -Test` |
| Fabric 서버 환경의 클래스 연결 및 전체 검사 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-runtime.ps1` |
| Fabric 클라이언트 환경의 클래스 연결 및 전체 검사 | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-runtime.ps1 -Environment CLIENT` |

`-Regression`은 컴파일과 해당 시나리오만 실행합니다. `-Test`는 분포·저장·복사·능력치·설정 등의 전체 검사를 실행하며 검증 항목 수가 많습니다.
`test-runtime.ps1`은 먼저 `build.ps1 -Test`로 생성한 클래스·클래스패스가 필요하며 실행 시 전체 검사를 다시 수행합니다. 두 환경 검사를 관성적으로 함께 실행하지 않습니다. 게임 창이나 월드는 실행하지 않습니다.

## Git에 남길 파일

소스, 리소스, 테스트 소스, 스크립트, 기본 설정, 문서, 라이선스를 커밋합니다.

`.gitignore`는 `.cache`, `.fabric`, `build`, `dist`, 로그·크래시 보고서·실행 폴더 등을 제외합니다. 테스트 소스는 재발 방지에 필요하므로 보존하고 실행 결과와 API 추출물은 커밋하지 않습니다. 개인 게임 폴더·월드·런처 계정 파일을 프로젝트에 복사하지 않습니다.

최신 설치용 패키지는 로컬 `dist`에 보관할 수 있지만 Git에는 포함하지 않습니다. 캐시와 `build`를 지워도 다음 승인된 빌드에서 다시 생성합니다. 모든 배포본을 지웠다면 소스에서 빌드·패키징해 재생성합니다.

Git 저장소 초기화와 실제 커밋은 사용자가 진행합니다. 정리 작업만으로 자동 초기화·커밋·푸시하지 않습니다.
