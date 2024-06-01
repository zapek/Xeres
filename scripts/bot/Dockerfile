FROM python:3.10-slim AS builder

WORKDIR /app/
COPY requirements.txt ./

RUN pip install --no-cache-dir -r requirements.txt

COPY bot.py .
COPY config.json .

ENV PYTHONUNBUFFERED=1

CMD [ "python", "./bot.py" ]