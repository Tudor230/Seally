# Nutrition API

Python backend that uses Perplexity's Sonar API to fetch nutritional macros for food items.

## Features

- **Full food descriptions**: Query using complete descriptions like "cheesy bacon fries burrito from Taco Bell"
- **Brand/restaurant support**: Works with specific menu items from chains and restaurants
- **Structured response**: Returns JSON with calories, protein, carbs, fat, fiber, and sugar
- **Real-time data**: Powered by Perplexity's web-grounded Sonar Pro API
- **Official SDK**: Uses the official `perplexityai` Python SDK

## Prerequisites

- Python 3.9+
- Perplexity API key (get one at https://www.perplexity.ai/api-platform)

## Setup

### 1. Clone and navigate to the project

```bash
cd sonar-server
```

### 2. Create a virtual environment

```bash
python -m venv venv
```

### 3. Activate the virtual environment

**Windows:**
```bash
venv\Scripts\activate
```

**macOS/Linux:**
```bash
source venv/bin/activate
```

### 4. Install dependencies

```bash
pip install -r requirements.txt
```

### 5. Configure environment variables

Copy the example environment file and add your API key:

```bash
copy .env.example .env
```

Edit `.env` and replace `your_api_key_here` with your actual Perplexity API key:

```env
PERPLEXITY_API_KEY=your_actual_api_key_here
```

**Note:** The `perplexityai` SDK automatically reads the `PERPLEXITY_API_KEY` environment variable.

## Running the Server

### Using Python directly

```bash
python -m app.main
```

### Using uvicorn

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The API will be available at `http://localhost:8000`

## API Endpoints

### Health Check

```bash
GET /health
```

### Lookup Nutrition (GET)

```bash
GET /nutrition/lookup?q={food_description}
```

**Parameters:**
- `q` (query): Full description of the food item

**Example:**
```bash
curl "http://localhost:8000/nutrition/lookup?q=cheesy%20bacon%20fries%20burrito%20from%20Taco%20Bell"
curl "http://localhost:8000/nutrition/lookup?q=Big%20Mac"
curl "http://localhost:8000/nutrition/lookup?q=homemade%20chocolate%20chip%20cookies"
```

### Lookup Nutrition (POST)

```bash
POST /nutrition/lookup
```

**Request Body:**
```json
{
  "food_query": "cheesy bacon fries burrito from Taco Bell"
}
```

**Example:**
```bash
curl -X POST "http://localhost:8000/nutrition/lookup" \
  -H "Content-Type: application/json" \
  -d '{"food_query": "cheesy bacon fries burrito from Taco Bell"}'
```

## Response Format

```json
{
  "food_query": "cheesy bacon fries burrito from Taco Bell",
  "macros": {
    "calories": 850,
    "protein": 28,
    "carbohydrates": 65,
    "fat": 52,
    "fiber": 6,
    "sugar": 4
  }
}
```

## API Documentation

Once the server is running, visit:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## Project Structure

```
sonar-server/
├── app/
│   ├── __init__.py
│   ├── config.py              # Configuration and settings
│   ├── main.py                # FastAPI application
│   └── services/
│       ├── __init__.py
│       └── perplexity_service.py  # Sonar API client
├── routes/
│   ├── __init__.py
│   └── nutrition.py           # Nutrition endpoints
├── .env                       # Environment variables (create from .env.example)
├── .env.example               # Environment template
├── .gitignore
├── requirements.txt
└── README.md
```

## Troubleshooting

### API Key Error
Make sure your `PERPLEXITY_API_KEY` is correctly set in the `.env` file.

### Module Not Found
Ensure you've installed all dependencies: `pip install -r requirements.txt`

### Port Already in Use
Change the port in `.env` or run with a different port:
```bash
uvicorn app.main:app --port 8001
```
