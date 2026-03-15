import json
import os
import logging
from typing import Optional
from pydantic import BaseModel
from perplexity import Perplexity

logger = logging.getLogger('uvicorn.error')


class NutritionMacros(BaseModel):
    """Nutritional information model."""

    calories: Optional[float] = None
    protein: Optional[float] = None
    carbohydrates: Optional[float] = None
    fat: Optional[float] = None
    fiber: Optional[float] = None
    sugar: Optional[float] = None


class PerplexityService:
    """Service for interacting with Perplexity Sonar API."""

    MODEL = "sonar-pro"

    def __init__(self):
        api_key = os.getenv("PERPLEXITY_API_KEY")
        if not api_key:
            raise ValueError("PERPLEXITY_API_KEY environment variable is not set")
        self.client = Perplexity(api_key=api_key)

    async def query_nutrition(self, food_query: str) -> NutritionMacros:
        """
        Query nutritional information for a food item.

        Args:
            food_query: Full description of the food item (e.g., "cheesy bacon fries burrito from Taco Bell")

        Returns:
            NutritionMacros object with parsed nutritional data
        """
        query = self._build_nutrition_query(food_query)

        logger.debug(f"Querying nutrition for: {food_query}")
        logger.debug(f"User prompt: {query}")

        completion = self.client.chat.completions.create(
            model=self.MODEL,
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are a nutrition expert. Provide accurate nutritional information "
                        "for food items. Return data in JSON format with these exact keys: "
                        "calories, protein, carbohydrates, fat, fiber, sugar. "
                        "Try to estimate unknown values."
                    ),
                },
                {"role": "user", "content": query},
            ],
            temperature=0.2,
            max_tokens=500,
        )

        content = completion.choices[0].message.content
        logger.debug(f"=== API RESPONSE ===")
        logger.debug(content)
        logger.debug(f"===================")
        return self._parse_nutrition_response(content)

    def _build_nutrition_query(self, food_query: str) -> str:
        """Build the query for nutritional information."""
        return (
            f"Provide detailed nutritional information for {food_query}. "
            "Include calories, protein, carbohydrates, fat, fiber, and sugar. "
            "Return ONLY a valid JSON object with these keys: calories, protein, carbohydrates, "
            "fat, fiber, sugar."
        )

    def _parse_nutrition_response(self, content: str) -> NutritionMacros:
        """Parse the LLM response into a NutritionMacros object."""
        logger.debug(f"Raw response: {content}")
        try:
            json_match = content.strip()
            if json_match.startswith("```json"):
                json_match = json_match[7:]
            if json_match.endswith("```"):
                json_match = json_match[:-3]
            json_match = json_match.strip()

            nutrition_data = json.loads(json_match)
            return NutritionMacros(
                calories=nutrition_data.get("calories"),
                protein=nutrition_data.get("protein"),
                carbohydrates=nutrition_data.get("carbohydrates"),
                fat=nutrition_data.get("fat"),
                fiber=nutrition_data.get("fiber"),
                sugar=nutrition_data.get("sugar"),
            )
        except (json.JSONDecodeError, KeyError) as e:
            logger.error(f"Parse error: {str(e)}")
            raise ValueError(f"Failed to parse nutrition data: {str(e)}")


perplexity_service = PerplexityService()
