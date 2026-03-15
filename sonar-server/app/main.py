import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

load_dotenv()

# Configure logging
logger = logging.getLogger('uvicorn.error')
logger.setLevel(logging.DEBUG)

from routes.nutrition import router as nutrition_router


app = FastAPI(
    title="Nutrition API",
    description="Backend API for fetching nutritional macros using Perplexity Sonar API",
    version="1.0.0",
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "version": "1.0.0"}


# Include routers
app.include_router(nutrition_router, prefix="/nutrition", tags=["Nutrition"])


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
