-- 운영자 계정을 ADMIN으로 승격한다.
-- 대상 이메일은 flyway placeholder(`spring.flyway.placeholders.adminEmail`)로 주입하며,
-- 이 리포지토리는 public이므로 실제 이메일 문자열을 파일에 남기지 않는다.
-- placeholder가 비어 있거나 해당 이메일의 계정이 아직 없으면 0행 갱신으로 조용히 no-op이다.
UPDATE users SET role = 'ADMIN' WHERE email = '${adminEmail}';
