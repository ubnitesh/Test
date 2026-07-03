# --- Stage 1: install dependencies ---
FROM python:3.11-slim AS builder

WORKDIR /build

COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

# --- Stage 2: lean runtime image ---
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    HOST=0.0.0.0 \
    PORT=8080

RUN groupadd --gid 1000 rapp \
    && useradd --uid 1000 --gid rapp --create-home --shell /usr/sbin/nologin rapp

WORKDIR /home/rapp

COPY --from=builder /install /usr/local
COPY app/ ./app/

USER rapp

EXPOSE 8080

CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
