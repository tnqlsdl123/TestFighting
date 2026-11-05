# 회원가입 API

`POST /api/users/signup` · `Content-Type: application/json`

비로그인 요청을 허용한다. 성공 HTTP 상태는 `200 OK`이며 회원가입 후 자동 로그인하지 않는다.

## 요청

```json
{
  "username": "test1234",
  "password": "password123!",
  "nickname": "수빈"
}
```

모든 필드는 필수 문자열이며 검증·중복 확인 전에 양끝 공백을 제거한다. 비밀번호도 같은 규칙을 적용한다.

| 필드 | 검증 규칙 |
| --- | --- |
| username | 2~10자. 대소문자를 구분하여 중복을 검사한다. 허용 문자에 대한 별도 제한은 없다. |
| password | 2자 이상. 영문과 숫자를 각각 하나 이상 포함한다. ASCII 특수문자도 허용하며 내부 공백과 한글은 허용하지 않는다. PBKDF2로 해시하여 저장한다. |
| nickname | 2~255자. 한글(자음·모음 포함), 영문, 숫자, 일반 공백을 허용한다. 특수문자와 탭·줄바꿈은 불가하며 중복을 허용하지 않는다. 내부 공백은 보존한다. |

닉네임 최대 255자 및 영문·숫자 허용은 미지정 부분에 적용한 구현 기준이다. 닉네임의 중복 판정도 현재는 대소문자를 구분한다.

## 세션 및 CSRF

세션·쿠키 방식에 맞춰 CSRF 보호를 유지한다. 로그인 기능은 이번 회원가입 구현 범위에 포함하지 않는다.

1. `GET /api/csrf`를 요청하여 세션 쿠키와 CSRF 토큰을 받는다.
2. 응답의 `result.headerName` 헤더에 `result.token`을 넣고 같은 세션 쿠키로 회원가입을 요청한다.
3. 브라우저에서 다른 origin의 API를 호출한다면 두 요청 모두 `credentials: "include"`를 사용한다.

CSRF 응답 예시:

```json
{
  "isSuccess": true,
  "code": "COMMON_200",
  "message": "요청에 성공했습니다.",
  "result": {
    "headerName": "X-CSRF-TOKEN",
    "token": "발급된 토큰"
  }
}
```

CSRF 토큰 누락·불일치 시 Spring Security에서 `403 Forbidden`으로 거부한다. 이 응답은 전역 예외 처리기를 거치지 않으며 `ApiResponse` 형식을 보장하지 않는다.

## 성공 응답

```json
{
  "isSuccess": true,
  "code": "COMMON_200",
  "message": "회원가입에 성공했습니다.",
  "result": {
    "id": 1,
    "username": "test1234",
    "nickname": "수빈"
  }
}
```

`id`는 DB에서 생성한 숫자다. 비밀번호 및 해시값은 응답하지 않는다.

## 실패 응답

| HTTP 상태 | code | 조건 | message |
| --- | --- | --- | --- |
| 400 | COMMON_400 | 필수값 누락, 길이·형식 검증 실패 | 첫 번째 필드 오류의 검증 문구 |
| 400 | COMMON_400 | 본문 누락 또는 읽을 수 없는 JSON | 잘못된 요청입니다. |
| 409 | USER_4091 | 아이디 중복 | 이미 사용 중인 아이디입니다. |
| 409 | USER_4092 | 닉네임 중복 | 이미 사용 중인 닉네임입니다. |
| 500 | COMMON_500 | 처리하지 못한 서버 오류 | 서버 에러가 발생했습니다. |

```json
{
  "isSuccess": false,
  "code": "COMMON_400",
  "message": "아이디는 2자 이상 10자 이하여야 합니다.",
  "result": null
}
```

모든 전역 예외 응답은 `isSuccess: false`, `result: null`을 반환한다. 여러 검증 오류 중 어떤 필드가 먼저 반환되는지는 보장하지 않는다. 전역 처리기에는 `COMMON_404`, `COMMON_405`도 정의되어 있으나 보안 필터에서 먼저 차단한 요청은 해당 응답이 나오지 않을 수 있다.

## 실행 환경 및 DB

- Java 17, MySQL 및 `fighting_db` 데이터베이스가 필요하다.
- `DB_USERNAME`, `DB_PASSWORD` 환경변수를 설정한다.
- `CORS_ALLOWED_ORIGINS`에는 허용할 프런트엔드 origin을 쉼표로 구분하여 설정한다. 기본값은 `http://localhost:3000`이다.
- HTTPS 운영 환경에서는 `SESSION_COOKIE_SECURE=true`로 설정한다. 세션 쿠키의 기본 SameSite는 `Lax`다. 서로 다른 사이트에 배포한다면 쿠키 정책을 별도로 구성해야 한다.
- `users.username`, `users.nickname`은 `utf8mb4_bin` collation과 이름 있는 UNIQUE 제약을 사용한다. 동시 중복 요청도 DB 제약으로 차단한다.
- 기존 `users` 테이블이 있다면 `ddl-auto=update`에 의존하지 말고 collation과 `uk_users_username`, `uk_users_nickname` 제약이 실제로 적용되어 있는지 확인하고 마이그레이션해야 한다.

테스트는 `./gradlew test`로 실행한다. 테스트 프로필의 H2 스키마는 대소문자를 구분하지만 MySQL collation 및 실제 동시 요청 동작까지 검증하지는 않는다.
