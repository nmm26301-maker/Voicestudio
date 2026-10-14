FROM python:3.12-slim
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY server/cloud_requirements.txt server/cloud_requirements.txt
RUN pip install --no-cache-dir -r server/cloud_requirements.txt
COPY server/cloud_gateway.py server/__init__.py server/
ENV VOICE_REFERENCE_DIR=/references
EXPOSE 8765
CMD ["uvicorn", "server.cloud_gateway:app", "--host", "0.0.0.0", "--port", "8765"]
