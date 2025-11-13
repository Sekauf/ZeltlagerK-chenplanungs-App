package de.zeltlager.kuechenplaner.data.repository.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.zeltlager.kuechenplaner.data.model.Ingredient;
import de.zeltlager.kuechenplaner.data.model.Recipe;
import de.zeltlager.kuechenplaner.data.model.RecipeWithIngredients;
import de.zeltlager.kuechenplaner.data.persistence.entity.RecipeEntity;
import de.zeltlager.kuechenplaner.data.persistence.entity.RecipeIngredientEntity;
import de.zeltlager.kuechenplaner.data.persistence.entity.UserEntity;
import de.zeltlager.kuechenplaner.data.persistence.repository.RecipeEntityRepository;
import de.zeltlager.kuechenplaner.data.repository.RecipeRepository;
import de.zeltlager.kuechenplaner.user.UserAccountService;

@Repository
@Profile("!memory")
@Transactional
public class JpaRecipeRepository implements RecipeRepository {

    private final RecipeEntityRepository recipeEntityRepository;
    private final UserAccountService userAccountService;

    public JpaRecipeRepository(RecipeEntityRepository recipeEntityRepository,
                               UserAccountService userAccountService) {
        this.recipeEntityRepository = recipeEntityRepository;
        this.userAccountService = userAccountService;
    }

    @Override
    public RecipeWithIngredients create(Recipe recipe, List<Ingredient> ingredients) {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        RecipeEntity entity = mapToEntity(recipe, ingredients, user);
        RecipeEntity saved = recipeEntityRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecipeWithIngredients> findById(long id) {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        return recipeEntityRepository.findByIdAndUser_Id(id, user.getId()).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeWithIngredients> findAll() {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        return recipeEntityRepository.findAllByUser_IdOrderByNameAsc(user.getId()).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public RecipeWithIngredients update(Recipe recipe, List<Ingredient> ingredients) {
        long recipeId = recipe.getId().orElseThrow(() -> new IllegalArgumentException("Recipe ID must be present"));
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        RecipeEntity entity = recipeEntityRepository.findByIdAndUser_Id(recipeId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recipe with id " + recipeId + " does not exist"));

        entity.setName(recipe.getName());
        entity.setCategoryId(recipe.getCategoryId().orElse(null));
        entity.setBaseServings(recipe.getBaseServings());
        entity.setInstructions(recipe.getInstructions());

        entity.getIngredients().clear();
        for (Ingredient ingredient : ingredients) {
            RecipeIngredientEntity ingredientEntity = mapIngredientToEntity(ingredient, entity);
            entity.getIngredients().add(ingredientEntity);
        }

        RecipeEntity saved = recipeEntityRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public void delete(long id) {
        UserEntity user = userAccountService.ensureCurrentUserEntity();
        recipeEntityRepository.deleteByIdAndUser_Id(id, user.getId());
    }

    private RecipeEntity mapToEntity(Recipe recipe, List<Ingredient> ingredients, UserEntity user) {
        RecipeEntity entity = new RecipeEntity();
        entity.setUser(user);
        entity.setName(recipe.getName());
        entity.setCategoryId(recipe.getCategoryId().orElse(null));
        entity.setBaseServings(recipe.getBaseServings());
        entity.setInstructions(recipe.getInstructions());

        List<RecipeIngredientEntity> ingredientEntities = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            ingredientEntities.add(mapIngredientToEntity(ingredient, entity));
        }
        entity.getIngredients().addAll(ingredientEntities);
        return entity;
    }

    private RecipeIngredientEntity mapIngredientToEntity(Ingredient ingredient, RecipeEntity recipeEntity) {
        RecipeIngredientEntity entity = new RecipeIngredientEntity();
        entity.setRecipe(recipeEntity);
        entity.setName(ingredient.getName());
        entity.setUnit(ingredient.getUnit());
        entity.setAmountPerServing(ingredient.getAmountPerServing());
        entity.setNotes(ingredient.getNotes().orElse(null));
        return entity;
    }

    private RecipeWithIngredients mapToDomain(RecipeEntity entity) {
        Recipe recipe = new Recipe(
                entity.getId(),
                entity.getName(),
                entity.getCategoryId(),
                entity.getBaseServings(),
                entity.getInstructions(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());

        List<Ingredient> ingredients = entity.getIngredients().stream()
                .map(ingredientEntity -> new Ingredient(
                        ingredientEntity.getId(),
                        entity.getId(),
                        ingredientEntity.getName(),
                        ingredientEntity.getUnit(),
                        ingredientEntity.getAmountPerServing(),
                        ingredientEntity.getNotes()))
                .collect(Collectors.toUnmodifiableList());

        return new RecipeWithIngredients(recipe, ingredients);
    }
}
