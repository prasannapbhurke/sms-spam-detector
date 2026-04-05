# Use a stable Python version
FROM python:3.9-slim

# Set working directory
WORKDIR /app

# Upgrade pip
RUN pip install --upgrade pip

# Copy requirements file
COPY requirements.txt .

# Install NLTK and download its data first
# This helps with Docker layer caching and build failures
RUN pip install nltk==3.8.1
RUN python -c "import nltk; nltk.download('stopwords')"

# Install other dependencies from requirements.txt
RUN pip install -r requirements.txt

# Copy the rest of your application code
COPY . .

# Railway provides the PORT environment variable.
# Gunicorn will bind to the port specified by the $PORT env var.
CMD sh -c "gunicorn app:app --bind 0.0.0.0:$PORT"
