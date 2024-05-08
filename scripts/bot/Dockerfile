FROM python:3.10 AS builder

WORKDIR /app/
COPY requirements.txt ./

RUN pip install --no-cache-dir -r requirements.txt

COPY bot.py .
COPY config.json .

CMD [ "python", "./bot.py" ]