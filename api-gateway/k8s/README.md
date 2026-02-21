# Kubernetes — evaluation-service

Автомасштабирование, лимиты памяти, балансировка через Nginx Ingress.

## Что настроено

- **Deployment** — реплики, лимиты CPU/памяти (при превышении памяти под убивается и поднимается заново), liveness/readiness.
- **Service** — внутренний ClusterIP, трафик между подами распределяется round-robin.
- **HPA** — масштабирование по CPU (70%) и памяти (80%), от 2 до 10 подов; при падении нагрузки — плавное scale down.
- **Ingress (Nginx)** — вход в кластер по HTTP, балансировка запросов на поды.

## Требования

1. Кластер Kubernetes.
2. Установленный [Nginx Ingress Controller](https://kubernetes.github.io/ingress-nginx/deploy/).
3. Метрики для HPA (в большинстве облачных кластеров уже есть; для minikube — [metrics-server](https://github.com/kubernetes-sigs/metrics-server)).

## Быстрый старт

```bash
# Сборка образа (из корня FIRE или evaluation-service)
docker build -t evaluation-service:latest -f evaluation-service/Dockerfile .

# Или с registry
docker build -t your-registry/evaluation-service:latest -f evaluation-service/Dockerfile .
docker push your-registry/evaluation-service:latest
```

Подставь свой образ в `k8s/deployment.yaml` (поле `image`).

```bash
# Применить манифесты
kubectl apply -f evaluation-service/k8s/configmap.yaml
kubectl apply -f evaluation-service/k8s/secret.yaml   # или создать секрет вручную
kubectl apply -f evaluation-service/k8s/deployment.yaml
kubectl apply -f evaluation-service/k8s/service.yaml
kubectl apply -f evaluation-service/k8s/hpa.yaml
kubectl apply -f evaluation-service/k8s/ingress.yaml
```

Или одной командой:

```bash
kubectl apply -f evaluation-service/k8s/
```

## Локальная проверка (minikube)

```bash
minikube addons enable ingress
minikube addons enable metrics-server
# в deployment.yaml укажи imagePullPolicy: Never и image: evaluation-service:latest
eval $(minikube docker-env)
docker build -t evaluation-service:latest -f evaluation-service/Dockerfile .
kubectl apply -f evaluation-service/k8s/
# доступ: добавить в /etc/hosts запись для evaluation.local → minikube ip
```

## Параметры

- **Память**: request 512Mi, limit 1Gi — при превышении 1Gi под перезапускается (OOMKill).
- **HPA**: min 2, max 10 подов; scale up при CPU > 70% или памяти > 80%.
- **Ingress**: хост по умолчанию `evaluation.local` — поменяй в `ingress.yaml` под свой домен.
