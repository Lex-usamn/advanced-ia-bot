package me.iabot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LearningSystem {
    private BotAI bot;
    private Map<String, Integer> actionSuccessRates;
    private Map<String, Long> lastLearningUpdate;
    private int learningPoints;

    public LearningSystem(BotAI bot) {
        this.bot = bot;
        this.actionSuccessRates = new HashMap<>();
        this.lastLearningUpdate = new HashMap<>();
        this.learningPoints = 0;
        
        // Inicializar taxas de sucesso
        String[] actions = {"mover", "minerar", "construir", "craftar", "combater", "coletar", "explorar"};
        for (String action : actions) {
            actionSuccessRates.put(action, 50); // 50% inicial
        }
    }

    public void update() {
        long now = System.currentTimeMillis();
        String currentTask = bot.getCurrentTask();
        
        // Atualizar aprendizado a cada 30 segundos
        Long lastUpdate = lastLearningUpdate.getOrDefault(currentTask, 0L);
        if (now - lastUpdate > 30000) {
            analyzePerformance(currentTask);
            lastLearningUpdate.put(currentTask, now);
        }
    }

    private void analyzePerformance(String action) {
        // Análise simplificada baseada em experiência ganha
        int currentRate = actionSuccessRates.getOrDefault(action, 50);
        int newRate = currentRate;
        
        // Se o bot está ganhando muita experiência, a ação é eficaz
        if (bot.getExperience() > 50) {
            newRate = Math.min(100, currentRate + 5);
        } else if (bot.getExperience() < 10) {
            newRate = Math.max(0, currentRate - 3);
        }
        
        actionSuccessRates.put(action, newRate);
        learningPoints += 1;
        
        // Melhorar habilidades com base no aprendizado
        if (learningPoints >= 10) {
            improveAdaptiveSkills();
            learningPoints = 0;
        }
    }

    private void improveAdaptiveSkills() {
        // Melhorar a habilidade com maior taxa de sucesso
        String bestAction = actionSuccessRates.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("idle");
            
        // Melhorar a skill correspondente
        String skill = mapActionToSkill(bestAction);
        // A implementação real seria mais complexa
    }

    private String mapActionToSkill(String action) {
        switch (action) {
            case "minerar": return "mining";
            case "construir": return "building";
            case "craftar": return "crafting";
            case "combater": return "combat";
            case "explorar": return "exploration";
            default: return "exploration";
        }
    }

    public int getActionSuccessRate(String action) {
        return actionSuccessRates.getOrDefault(action, 50);
    }

    public void recordActionSuccess(String action, boolean success) {
        int currentRate = actionSuccessRates.getOrDefault(action, 50);
        int newRate = success ? Math.min(100, currentRate + 2) : Math.max(0, currentRate - 2);
        actionSuccessRates.put(action, newRate);
    }

    public Map<String, Integer> getActionSuccessRates() {
        return new HashMap<>(actionSuccessRates);
    }

    public void resetLearning() {
        actionSuccessRates.replaceAll((k, v) -> 50);
        learningPoints = 0;
    }
}