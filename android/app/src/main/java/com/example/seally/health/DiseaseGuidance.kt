package com.example.seally.health

import com.example.seally.camera.ExerciseType

data class DiseaseDefinition(
    val id: String,
    val label: String,
    val foodKeywords: Set<String>,
    val restrictedExercises: Set<ExerciseType>,
)

object DiseaseGuidance {
    private val commonDiseaseDefinitions = listOf(
        DiseaseDefinition(
            id = "hypertension",
            label = "Hypertension",
            foodKeywords = setOf("salt", "salty", "sodium", "bacon", "sausage", "chips", "fast food", "soy sauce", "pickle"),
            restrictedExercises = setOf(ExerciseType.PULLUP, ExerciseType.PUSHUP),
        ),
        DiseaseDefinition(
            id = "heart_disease",
            label = "Heart Disease",
            foodKeywords = setOf("fried", "butter", "bacon", "sausage", "burger", "fast food", "cream", "processed"),
            restrictedExercises = setOf(ExerciseType.PULLUP, ExerciseType.PUSHUP),
        ),
        DiseaseDefinition(
            id = "type2_diabetes",
            label = "Type 2 Diabetes",
            foodKeywords = setOf("soda", "candy", "cake", "cookie", "donut", "sugar", "sweet", "juice", "syrup"),
            restrictedExercises = emptySet(),
        ),
        DiseaseDefinition(
            id = "obesity",
            label = "Obesity",
            foodKeywords = setOf("fried", "soda", "fast food", "candy", "cake", "cookie", "donut", "chips"),
            restrictedExercises = setOf(ExerciseType.PUSHUP),
        ),
        DiseaseDefinition(
            id = "high_cholesterol",
            label = "High Cholesterol",
            foodKeywords = setOf("fried", "butter", "cheese", "bacon", "burger", "sausage", "cream"),
            restrictedExercises = emptySet(),
        ),
        DiseaseDefinition(
            id = "asthma",
            label = "Asthma",
            foodKeywords = setOf("soda", "energy drink", "syrup"),
            restrictedExercises = setOf(ExerciseType.PULLUP, ExerciseType.PUSHUP),
        ),
        DiseaseDefinition(
            id = "arthritis",
            label = "Arthritis",
            foodKeywords = setOf("fried", "soda", "processed", "chips", "sugar"),
            restrictedExercises = setOf(ExerciseType.SQUAT, ExerciseType.PUSHUP),
        ),
        DiseaseDefinition(
            id = "kidney_disease",
            label = "Kidney Disease",
            foodKeywords = setOf("salt", "sodium", "processed", "canned", "deli", "pickle"),
            restrictedExercises = setOf(ExerciseType.PULLUP),
        ),
        DiseaseDefinition(
            id = "celiac_disease",
            label = "Celiac Disease",
            foodKeywords = setOf("bread", "pasta", "noodle", "wheat", "barley", "rye", "flour"),
            restrictedExercises = emptySet(),
        ),
        DiseaseDefinition(
            id = "gout",
            label = "Gout",
            foodKeywords = setOf("beer", "alcohol", "organ meat", "liver", "anchovy", "sardine", "seafood"),
            restrictedExercises = setOf(ExerciseType.SQUAT),
        ),
        DiseaseDefinition(
            id = "ibs",
            label = "IBS",
            foodKeywords = setOf("onion", "garlic", "milk", "cheese", "bean", "lentil", "wheat"),
            restrictedExercises = emptySet(),
        ),
        DiseaseDefinition(
            id = "gerd",
            label = "GERD",
            foodKeywords = setOf("coffee", "chocolate", "mint", "tomato", "spicy", "fried", "citrus"),
            restrictedExercises = emptySet(),
        ),
        DiseaseDefinition(
            id = "osteoporosis",
            label = "Osteoporosis",
            foodKeywords = setOf("soda", "processed", "salt"),
            restrictedExercises = setOf(ExerciseType.SQUAT),
        ),
        DiseaseDefinition(
            id = "anemia",
            label = "Anemia",
            foodKeywords = setOf("tea", "coffee", "bran"),
            restrictedExercises = setOf(ExerciseType.PULLUP),
        ),
    )

    private val diseaseById = commonDiseaseDefinitions.associateBy { it.id }

    val commonDiseases: List<DiseaseDefinition> = commonDiseaseDefinitions

    fun diseaseLabels(ids: Set<String>): List<String> = ids.mapNotNull { diseaseById[it]?.label }

    fun foodNotRecommendedFor(foodName: String, diseaseIds: Set<String>): List<String> {
        val normalized = foodName.trim().lowercase()
        if (normalized.isBlank() || diseaseIds.isEmpty()) return emptyList()
        return diseaseIds.mapNotNull { diseaseById[it] }
            .filter { def -> def.foodKeywords.any { normalized.contains(it) } }
            .map { it.label }
    }

    fun exerciseNotRecommendedFor(exerciseType: ExerciseType, diseaseIds: Set<String>): List<String> {
        if (diseaseIds.isEmpty()) return emptyList()
        return diseaseIds.mapNotNull { diseaseById[it] }
            .filter { def -> def.restrictedExercises.contains(exerciseType) }
            .map { it.label }
    }

    fun exerciseNotRecommendedForName(exerciseName: String, diseaseIds: Set<String>): List<String> {
        val exerciseType = exerciseTypeFromName(exerciseName) ?: return emptyList()
        return exerciseNotRecommendedFor(exerciseType, diseaseIds)
    }

    private fun exerciseTypeFromName(exerciseName: String): ExerciseType? {
        val normalized = exerciseName.trim().lowercase()
        if (normalized.isBlank()) return null
        return when {
            normalized.contains("squat") || normalized.contains("lunge") || normalized.contains("step-up") || normalized.contains("step up") ||
                normalized.contains("wall sit") || normalized.contains("stair climbing") || normalized.contains("running") ||
                normalized.contains("walking") -> ExerciseType.SQUAT
            normalized.contains("plank") || normalized.contains("mountain climber") || normalized.contains("bear crawl") ||
                normalized.contains("bicycle crunch") || normalized.contains("sit-up") || normalized.contains("sit up") ||
                normalized.contains("crunch") || normalized.contains("leg raise") || normalized.contains("glute bridge") ||
                normalized.contains("superman hold") -> ExerciseType.PLANK
            normalized.contains("pull-up") || normalized.contains("pull up") || normalized.contains("pullup") -> ExerciseType.PULLUP
            normalized.contains("push-up") || normalized.contains("push up") || normalized.contains("pushup") || normalized.contains("burpee") ||
                normalized.contains("jumping jack") || normalized.contains("high knees") || normalized.contains("skipping rope") ||
                normalized.contains("shadow boxing") -> ExerciseType.PUSHUP
            else -> null
        }
    }
}


