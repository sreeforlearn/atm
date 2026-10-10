# Jenkins Setup — ATM Enterprise

This guide covers installing and configuring Jenkins for the ATM
Enterprise CI/CD pipelines (`cicd/jenkins/Jenkinsfile` for full
builds on `main`, `Jenkinsfile.pr` for PR verification).

---

## 1. Run Jenkins

Quickest local setup with Docker:

```bash
docker run -d --name jenkins \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $(which docker):/usr/bin/docker \
  --group-add $(getent group docker | cut -d: -f3) \
  jenkins/jenkins:lts-jdk17
```

> The Docker socket bind lets Jenkins spawn sibling containers
> for `docker.build(...)`. This is fine for dev; in production
> use [DinD](https://www.jenkins.io/doc/book/pipeline/docker/)
> or a remote Docker host.

For Kubernetes, the official chart is preferred:

```bash
helm repo add jenkins https://charts.jenkins.io
helm repo update
helm install jenkins jenkins/jenkins \
  --namespace jenkins --create-namespace \
  --set controller.runAsUser=1000 \
  --set controller.ingress.enabled=true \
  --set controller.ingress.hostName=jenkins.atm.local
```

## 2. Apply Configuration-as-Code

Mount `jenkins-casc.yaml` and set `CASC_JENKINS_CONFIG`:

```bash
# Inside the Jenkins container
mkdir -p /var/jenkins_home/casc_configs
cp cicd/jenkins/jenkins-casc.yaml /var/jenkins_home/casc_configs/
# Restart Jenkins, or use: Manage Jenkins → Configuration as Code → Reload
```

## 3. Required Plugins

Install via *Manage Jenkins → Plugins → Available plugins* or
via the `install-plugins.sh` entrypoint (see `jenkins-casc.yaml`
for the canonical list). Minimum set:

- `kubernetes` (for K8s agent / deploy)
- `docker-workflow` + `docker`
- `workflow-aggregator` (Pipeline DSL)
- `blueocean`
- `sonar` (SonarQube plugin)
- `maven-plugin`, `config-file-provider`
- `credentials-binding`
- `ansicolor`, `timestamper`, `junit`

## 4. Required Credentials

Create the following under *Manage Jenkins → Credentials → System →
Global credentials*:

| ID                  | Kind              | Purpose                                     |
|---------------------|-------------------|---------------------------------------------|
| `docker-hub`        | Username/Password | Push/pull to Docker Hub                     |
| `docker-hub-registry` | Secret text      | Registry namespace, e.g. `docker.io/myuser` |
| `sonar-token`       | Secret text       | SonarQube analysis token                    |
| `kubeconfig-atm`    | Kubernetes config | Deploy to the atm-dev cluster              |
| `github-app`        | GitHub App creds  | For multibranch scan (optional)             |

## 5. Tool Installations

Define under *Manage Jenkins → Tools* (or via JCasC):

- **JDK** — `JDK-17` (Temurin)
- **Maven** — `Maven-3.9`

## 6. Webhooks

- **GitHub webhook**: `https://jenkins.atm.local/github-webhook/`
  Content-Type `application/json`, triggers: `push`, `pull_request`.
- **PR pipeline** uses a `Multibranch Pipeline` job pointing at
  `cicd/jenkins/Jenkinsfile.pr`.

## 7. K8s Cluster Access

Store the kubeconfig of the target cluster as the `kubeconfig-atm`
credential. The pipeline's `Deploy to K8s` stage does:

```bash
kubectl apply -f k8s/namespaces/
kubectl apply -f k8s/config/
kubectl apply -f k8s/postgres/
...
kubectl -n atm-dev rollout status deployment/auth-service
```

## 8. SonarQube

Set up a SonarQube server (or use SonarCloud). Add a token,
save it as `sonar-token`. Define a SonarQube installation named
`SonarQube` under *Manage Jenkins → System → SonarQube servers*.

## 9. Troubleshooting

| Symptom                                          | Fix                                               |
|--------------------------------------------------|---------------------------------------------------|
| `docker: command not found` in pipeline          | Bind `/usr/bin/docker` into the Jenkins container |
| `permission denied` on `/var/run/docker.sock`    | Add Jenkins user to the host `docker` group       |
| `kubectl: command not found`                     | `apt install kubectl` inside the Jenkins image    |
| Kubeconfig credential rejected                   | Ensure the file's current-context is named `atm`  |
| Sonar analysis fails with 401                    | Verify `sonar-token` and `sonar.host.url`         |
| Pipeline times out during `rollout status`       | Check pod logs: `kubectl -n atm-dev describe pod` |
