# Use a stable Python version
FROM python:3.9-slim

# Set working directory
WORKDIR /app

# Upgrade pip
RUN pip install --upgrade pip

# Copy requirements file
COPY requirements.txt .

# Install NLTK and download its data first
RUN pip install nltk==3.8.1
RUN python -c "import nltk; nltk.download('stopwords')"

# Install other dependencies from requirements.txt
RUN pip install -r requirements.txt

# Copy the rest of your application code
COPY . .

# The CMD instruction will be run by a shell, which correctly handles the $PORT variable.
# We use "python -m gunicorn" as the most reliable way to run gunicorn.
CMD python -m gunicorn app:app --bind 0.0.0.0:$PORT
