# EC2 Docker 배포 계획

> 목적: `main` 브랜치 push를 트리거로 GitHub Actions에서 빌드, Docker Hub 이미지 push, EC2 SSH 배포를 수행한다.
> 범위: 백엔드 앱 컨테이너와 EC2 내부 MySQL 컨테이너 배포. 사용자 업로드 이미지 S3 저장은 별도 기능 작업으로 분리한다.

## 1. 배포 흐름

1. `main` 브랜치에 push 또는 PR merge
2. GitHub Actions 실행
3. JDK 21 설정
4. `./gradlew clean bootJar -x test` 실행
   - Spring 테스트는 배포 파이프라인에서 실행하지 않는다.
   - 컴파일과 실행 jar 생성만 검증한다.
5. `build/libs/*.jar`를 `app.jar`로 복사
6. Docker image 빌드
7. Docker Hub에 `${DOCKERHUB_USERNAME}/dp-be:latest` push
8. EC2 `/home/ec2-user`로 아래 파일 업로드
   - `deploy.sh`
   - `.env`
   - `docker-compose.yml`
   - `schema.sql`
9. GitHub Actions가 EC2에 SSH 접속
10. EC2에서 `deploy.sh` 실행
11. EC2 Docker가 최신 앱 이미지를 pull하고 `docker compose up -d`로 재기동

## 2. 추가된 파일

- `.github/workflows/deploy.yml`
  - GitHub Actions 배포 workflow
- `Dockerfile`
  - GitHub Actions에서 생성한 `app.jar`를 포함해 앱 이미지를 만든다.
- `.dockerignore`
  - Docker build context에서 불필요한 파일을 제외한다.
- `infra/deploy/docker-compose.yml`
  - EC2 운영용 compose 파일
  - `app`, `mysql` 서비스를 포함한다.
- `infra/deploy/scripts/deploy.sh`
  - EC2에서 Docker 설치 확인, 이미지 pull, compose 재기동, 이미지 정리를 수행한다.
- `src/main/java/org/dpbe/global/config/CorsConfig.java`
  - `APP_CORS_ALLOWED_ORIGINS` 환경변수 기반 CORS 설정

## 3. GitHub Secrets

필수:

| Secret | 설명 |
|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub 계정명 |
| `DOCKERHUB_TOKEN` | Docker Hub access token |
| `EC2_HOST` | EC2 public IP 또는 domain |
| `EC2_USERNAME` | EC2 SSH 사용자. Amazon Linux 기본값은 `ec2-user` |
| `EC2_SSH_KEY` | EC2 접속용 private key |
| `ENV_FILE` | EC2에 업로드할 `.env` 전체 내용 |

Docker Hub 계정을 GitHub 계정과 연동해 사용하는 경우에도 GitHub Actions 로그인에는 Docker Hub 비밀번호가 아니라 access token을 쓰는 편이 안전하다. Docker Hub에서 생성한 token 값을 GitHub Secret `DOCKERHUB_TOKEN`에 등록한다.

## 4. ENV_FILE 예시

```env
DOCKER_IMAGE=your-dockerhub-username/dp-be:latest

APP_PORT=8080
SERVER_PORT=8080
APP_SEED_ENABLED=false
APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com,http://localhost:5173

MYSQL_ROOT_PASSWORD=change-root-password
MYSQL_DATABASE=insurance_db
MYSQL_USER=admin
MYSQL_PASSWORD=change-user-password
MYSQL_HOST_PORT=3306

SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/insurance_db?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=change-user-password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
```

주의:
- `DOCKER_IMAGE`는 workflow가 push하는 이미지와 같아야 한다.
- 운영 서버에서는 기본적으로 `APP_SEED_ENABLED=false`를 권장한다.
- 최초 개발 서버 데이터가 필요하면 일시적으로 `true`로 배포한 뒤 다시 `false`로 바꾼다.
- Docker Hub 로그인용 `DOCKERHUB_TOKEN`, EC2 접속용 `EC2_HOST`, `EC2_USERNAME`, `EC2_SSH_KEY`는 `.env`가 아니라 GitHub Secrets에 각각 등록한다.

## 5. EC2 사전 준비

필수 보안그룹:
- SSH: `22`
- 백엔드 API: `8080` 또는 `APP_PORT`로 지정한 포트
- MySQL: 외부 접속이 필요 없으면 `3306`은 열지 않는 것이 좋다.

`deploy.sh`가 Docker와 Docker Compose 설치를 확인하고 없으면 설치한다. 그래도 초기 서버에서는 아래가 준비되어 있으면 좋다.

Ubuntu/Debian 계열:

```bash
sudo apt-get update
sudo apt-get install -y docker.io
sudo systemctl enable docker
sudo systemctl start docker
```

Amazon Linux/RHEL 계열:

```bash
sudo yum install -y docker
sudo systemctl enable docker
sudo systemctl start docker
```

`deploy.sh`는 `apt-get`, `yum`, `dnf` 중 사용 가능한 패키지 매니저를 자동으로 선택한다.

## 6. 운영 compose 동작

`infra/deploy/docker-compose.yml`은 EC2에서 `/home/ec2-user/docker-compose.yml`로 배치된다.

서비스:
- `app`
  - Docker Hub의 `DOCKER_IMAGE`를 pull해서 실행
  - MySQL healthcheck 이후 시작
  - `APP_PORT:8080`으로 포트 매핑
- `mysql`
  - `mysql:8.0`
  - `mysql_data` volume에 데이터 유지
  - 최초 volume 생성 시 `/home/ec2-user/schema.sql`로 schema 초기화

볼륨:
- `mysql_data`: MySQL 데이터
- `dispatch_uploads`: 현재 로컬 업로드 파일 저장용

S3 전환 전까지 `dispatch_uploads`는 임시 로컬 저장소다. 사용자 업로드 이미지를 S3에 저장하도록 바꾸면 이 볼륨은 제거하거나 fallback 용도로만 남긴다.

## 7. 로컬 환경과 서버 환경 차이

로컬:
- 기본 `docker-compose.yml`의 `mysql`만 실행하면 된다.
- 앱은 보통 `./gradlew bootRun`으로 실행한다.
- 앱까지 Docker로 실행하려면 `app.jar` 생성 후 `docker compose --profile app up -d`를 사용한다.
- 루트 `docker-compose.yml`은 로컬 기본값을 가지고 있어 별도 `.env` 없이 실행할 수 있다.
- 로컬 기본 DB 값은 `MYSQL_DATABASE=insurance_db`, `MYSQL_USER=admin`, `MYSQL_PASSWORD=1234`, `MYSQL_ROOT_PASSWORD=root`이다.

서버:
- `infra/deploy/docker-compose.yml` 사용
- 앱과 MySQL 모두 Docker 컨테이너로 실행
- 설정값은 GitHub Secret `ENV_FILE`에서 전달된 `/home/ec2-user/.env`를 사용

## 8. 확인 명령

EC2에서 상태 확인:

```bash
cd /home/ec2-user
docker ps
docker logs dp_be --tail=100
docker logs insurance_db --tail=100
```

재배포 수동 실행:

```bash
cd /home/ec2-user
chmod +x deploy.sh
./deploy.sh
```
