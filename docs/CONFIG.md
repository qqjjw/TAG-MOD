# 설정 안내

플레이에 사용하는 파일은 **게임 디렉터리의 `config/tagmod.json`**입니다. 프로젝트의 같은 경로에 있는 파일은 배포용 기본 설정입니다. 프로젝트 파일만 고쳐서는 설치된 게임에 적용되지 않습니다.

두 사람은 같은 설정을 사용하고 변경 후 월드를 나갔다 다시 엽니다. LAN의 실제 게임 규칙은 호스트가 처리합니다. 설정 GUI·자동 공유·자동 재로드는 제공하지 않습니다.

## 몬스터

확률은 `0~1`입니다. `0.5`는 50%, `0.005`는 0.5%를 뜻합니다.

| 항목 | 기본값 | 설명 |
|---|---:|---|
| `mobChance` | 0.50 | 일반 대상의 포션 강화 확률 |
| `doubleTraitChance` | 0.20 | 강화된 일반 대상이 특성 2개를 받을 확률 |
| `bossChance` | 0.10 | 보스·워든 저항 I 확률 |
| `equipmentChance` | 0.20 | 장비 사용 몹의 추가 무장 확률 |
| `equipmentDropChance` | 0.005 | 추가 장비의 슬롯별 기본 드롭률, 약탈 보정 유지 |
| `largeMobChance` | 0.08 | 일반 대상의 대형화 확률, 보스·워든 제외 |
| `largeMobMinScale` / `largeMobMaxScale` | 1.6 / 2.0 | 기본 크기에 곱하는 배율, 허용 범위 1~4 |
| `invisibleMinSeconds` / `invisibleMaxSeconds` | 20 / 40 | 투명화 발동 간격 |
| `invisibleSeconds` | 2 | 투명화 지속 시간 |
| `mobEffectWeights` | 각 1 | 효과별 상대 가중치 |
| `extraMobTypes` / `excludedMobTypes` | `[]` | 추가 / 제외 몹 ID 목록, 제외 우선 |

`mobEffectWeights`의 키는 `speed`, `resistance`, `jump`, `strength`, `fire_resistance`, `invisibility`입니다. 특정 효과의 값을 0으로 두면 해당 후보를 제외합니다. 모든 후보 가중치가 0이면 일반 몹에 포션 특성을 부여하지 않습니다.

대형화·강화·추가 무장은 독립 추첨이므로 같은 몹에 함께 적용될 수 있습니다. 대형화는 체력·공격력을 별도로 올리지 않습니다. 이미 처리된 개체는 설정 변경 후에도 다시 추첨하지 않습니다.

## 장비

| 항목 | 기본값 | 설명 |
|---|---:|---|
| `beneficialWeight` / `harmfulWeight` / `mixedWeight` | 1 / 1 / 1 | 유리 / 불리 / 혼합 유형의 상대 가중치 |
| `potionOptionWeight` / `lifestealOptionWeight` | 1 / 1 | 포션 / 흡혈 옵션의 상대 가중치 |
| `severeEffectChance` | 0.10 | 부정 포션 옵션이 독·위더·실명·어둠·공중 부양에서 뽑힐 확률 |
| `lifestealRatio` / `lifestealCap` | 0.03 / 1 | 흡혈 비율 / 적중당 회복 체력 상한 |
| `lifestealCooldownTicks` | 20 | 흡혈 재사용 간격, 20틱 = 1초 |
| `ranges` | 아래 표 | 수치 옵션의 `[최솟값, 최댓값]` |
| `optionWeights` | 각 1 | 수치 옵션별 상대 가중치, 0은 후보 제외 |
| `extraItems` / `excludedItems` | `[]` | 추가 / 제외 아이템 ID 목록, 제외 우선. 내구도 있는 아이템만 대상 |

| `ranges` 키 | 기본 범위 | 단위 |
|---|---|---|
| `damage`, `speed`, `mining`, `draw` | `[0.05, 0.20]` | 피해·공격속도·채굴속도·장전속도 비율 |
| `armor`, `toughness` | `[0.05, 0.20]` | 방어력·방어 강도 비율 |
| `durability` | `[0.10, 0.30]` | 최대 내구도 비율 |
| `reach`, `block_reach` | `[0.25, 0.50]` | 블록 |
| `movement` | `[0.02, 0.05]` | 이동속도 비율 |
| `health` | `[1, 2]` | 체력 포인트, 2포인트 = 한 칸 |
| `knockback` | `[0.02, 0.08]` | 넉백 저항 가산량 |
| `safe_fall` | `[1, 2]` | 안전 낙하 거리, 블록 |
| `fall_damage` | `[0.05, 0.20]` | 낙하 피해 비율 |
| `water_mining` | `[0.05, 0.20]` | 수중 채굴속도 비율 |
| `water_move` | `[0.05, 0.20]` | 수중 이동 효율 가산량 |

범위에는 양수 크기를 적습니다. 추첨된 장단점에 따라 실제 부호를 정합니다. 예를 들어 유리한 낙하 피해 옵션은 피해 감소입니다. 장비 종류와 기본 속성에 따라 사용할 수 있는 후보를 제한합니다.

호환 후보의 가중치를 모두 0으로 만들면 장비가 미처리 상태로 남지 않도록 호환 후보를 균등 추첨합니다. 이미 저장된 장비 옵션과 흡혈 비율·상한·간격은 설정 변경만으로 재추첨되지 않습니다.

## 변경 예시

- 더 자주 무장시키기: `equipmentChance`를 `0.35`로 변경.
- 대형 몹 줄이기: `largeMobChance`를 `0.03`으로 변경.
- 대형화 끄기: `largeMobChance`를 `0`으로 변경.
- 투명화 끄기: `mobEffectWeights` 안의 `invisibility`를 `0`으로 변경.
- 나쁜 장비 비중 높이기: `harmfulWeight`를 `2`로 변경.
- 워든 제외: `excludedMobTypes`에 `"minecraft:warden"` 추가.

JSON에는 주석이나 마지막 항목 뒤의 쉼표를 넣지 않습니다. 잘못된 항목은 로그에 원인을 남기고 해당 기본값을 사용합니다. JSON 문법 자체가 잘못되면 원본을 덮어쓰지 않고 전체 기본값으로 실행합니다. 새 설정 키가 없는 기존 파일에는 해당 코드 기본값을 사용합니다.
