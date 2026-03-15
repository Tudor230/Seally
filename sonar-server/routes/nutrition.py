from typing import Optional

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

from app.services.perplexity_service import perplexity_service, NutritionMacros


router = APIRouter()


class NutritionRequest(BaseModel):
    """Request model for nutrition search."""

    food_query: str


class NutritionResponse(BaseModel):
    """Response model for nutrition data."""

    food_query: str
    macros: NutritionMacros


@router.get("/lookup", response_model=NutritionResponse, summary="Lookup nutrition by food description")
async def lookup_nutrition(
    q: str = Query(..., description="Full food description (e.g., 'cheesy bacon fries burrito from Taco Bell')")
):
    """
    Get nutritional information for a food item.

    - **q**: Full description of the food item (e.g., "cheesy bacon fries burrito from Taco Bell", "Big Mac", "homemade lasagna")
    """
    try:
        print(f"Received nutrition lookup request for: {q}")
        macros = await perplexity_service.query_nutrition(q)
        return NutritionResponse(
            food_query=q,
            macros=macros,
        )
    except ValueError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch nutrition data: {str(e)}")


@router.post("/lookup", response_model=NutritionResponse, summary="Lookup nutrition (POST)")
async def lookup_nutrition_post(request: NutritionRequest):
    """
    Get nutritional information for a food item using POST.

    - **food_query**: Full description of the food item
    """
    try:
        macros = await perplexity_service.query_nutrition(request.food_query)
        return NutritionResponse(
            food_query=request.food_query,
            macros=macros,
        )
    except ValueError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch nutrition data: {str(e)}")
