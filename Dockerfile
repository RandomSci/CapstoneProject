FROM python:3.11-slim
WORKDIR /PERCEPTRONX
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
RUN apt-get update && apt-get install -y \
    gcc \
    default-libmysqlclient-dev \
    pkg-config \
    libffi-dev \
    libssl-dev \
    libc-dev \
    curl \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxrender1 \
    libxext6 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
COPY Backend/requirements.txt .
RUN pip install --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt
COPY Backend /PERCEPTRONX/Backend
COPY Frontend_Web /PERCEPTRONX/Frontend_Web
COPY Frontend /PERCEPTRONX/Frontend
COPY docker-compose.yml .
COPY init.sql .
COPY start.sh /PERCEPTRONX/
COPY Frontend_Web/static/assets/images/user/ /PERCEPTRONX/Frontend_Web/static/assets/images/user/
RUN chmod -R 777 /PERCEPTRONX/Frontend_Web/static/assets/images/user
RUN chmod +x /PERCEPTRONX/start.sh

WORKDIR /PERCEPTRONX/Backend
EXPOSE 8000

CMD ["/bin/bash", "/PERCEPTRONX/start.sh"]
