package org.example;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    static List<Hint> availableHints = new ArrayList<>();
    static Hint activeHint;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        List<Question> questions = getQuestions();
        List<Level> levels = getLevels();

        availableHints = new ArrayList<>(Arrays.stream(Hint.values()).skip(1).toList());
        activeHint = Hint.None;

        int userScore = 0;

        System.out.println("Начинаем игру. По команде /hint доступен список подсказок.");

        for (var l : levels) {

            var currQuestion = getCurrentQuestion(questions, l.id());

            if (currQuestion == null)
                break;

            System.out.println(l.preface());
            System.out.println(currQuestion.text());

            displayChoices(currQuestion.choiceList(), currQuestion.answerId, Hint.None);

            System.out.println("Введите свой ответ (1, 2, 3 или 4) ниже: ");

            boolean isAnswered = processUserInput(currQuestion, scanner, currQuestion.answerId(),currQuestion.choiceList().size());

            if (!isAnswered) {
                System.out.printf("Неправильно, правильный ответ %s, ваш счет: %d", currQuestion.answerId(), userScore);
                return;
            } else {
                System.out.printf(String.format("%s  Ваш счет:  %s", l.afterword(), l.reward()));
                System.out.println();
            }

            userScore = l.reward();
        }

        System.out.println("Вы выиграли! Ваш счет: " + userScore);
    }

    static void displayChoices(List<String> choices, int answerId, Hint hint) {

        switch (hint) {
            case None:
                for (int i = 0; i < choices.size(); i++)
                    System.out.println((i + 1) + ": " + choices.get(i));

                break;

            case FiftyFifty:

                var hintChoices = new HashMap<>();
                hintChoices.put(answerId, choices.get(answerId - 1));

                int wrongChoiceId;
                var rand = new Random();

                do {
                    wrongChoiceId = rand.nextInt(choices.size());
                }
                while (wrongChoiceId == (answerId - 1));

                hintChoices.put(wrongChoiceId + 1, choices.get(wrongChoiceId));

                System.out.println("Варианты ответа после подсказки 50х50: ");

                for (var hintChoice : hintChoices.entrySet()) {
                    System.out.println(hintChoice.getKey() + ": " + hintChoice.getValue());
                }

                break;
            case FriendCall:
                System.out.println("Твой друг думает, что ответ - " + answerId);
                break;
            case AudienceHelp:
                System.out.println("Зрители думают, что ответ - " + answerId);
                break;
        }
    }

    static boolean processUserInput(Question question, Scanner scanner, int answerId, int answerRange)
    {
        Optional<Integer> userAnswer = Optional.empty();

        do {
            String input = scanner.nextLine();
            input = input.trim();

            if (input.startsWith("/"))
                processUserCommand(question, input.substring(1));
            else {
                try
                {
                    int answer = Integer.parseInt(input);

                    if (answer <= 0 || answer > answerRange)
                        throw new NumberFormatException();

                    userAnswer = Optional.of(answer);
                }
                catch (NumberFormatException e) {
                    System.out.println("Введите команду или число от 1 до 4х (ваш ответ). Это не ответ: " + input);
                }
            }

        } while (userAnswer.isEmpty());

        return userAnswer.get().equals(answerId);
    }

    static void processUserCommand(Question question, String command)
    {
        if (command.isEmpty())
        {
            System.out.println("Неизвестная команда");
            return;
        }

        String[] commandArr = command.split(" ");
        String commandType = commandArr[0];

        String commandArgs = commandArr.length > 1 ? commandArr[1] : null;

        switch (commandType) {
            case "hint":
                if (commandArgs != null && !commandArgs.isEmpty()) {

                    int hintId;

                    try {
                        hintId = Integer.parseInt(commandArgs);
                    } catch (Exception e) {
                        System.out.println("Неизвестная подсказка");
                        return;
                    }

                    if (hintId <= 0 || hintId > availableHints.size()) {
                        System.out.println("Нет такой подсказки");
                        return;
                    }

                    var hint = availableHints.get(hintId - 1);
                    availableHints.remove(hint);

                    displayChoices(question.choiceList(), question.answerId(), hint);
                } else {
                    displayHints();
                }

                // display available hints
                break;
            case "help":
                System.out.println("Введите /hint чтобы увидеть доступные подсказки.\nВведите /hint <число> чтобы получить подсказку");
                break;
            default:
                System.out.println("Неизвестная команда, введите /help для отображения списка команд.");
        }
    }

    static void displayHints()
    {
        if (availableHints.isEmpty()) {
            System.out.println("Подсказок больше нет!");
            return;
        }

        System.out.println("Доступные подсказки:");

        for (int i = 0; i < availableHints.size(); i++) {
            Hint h = availableHints.get(i);

            switch (h)
            {
                case Hint.AudienceHelp -> System.out.println((i + 1) + " Помощь зала");
                case Hint.FiftyFifty -> System.out.println((i + 1) + " 50 на 50");
                case Hint.FriendCall -> System.out.println((i + 1) + " Звонок другу");
            }
        }
    }

    static Question getCurrentQuestion(List<Question> questions, int levelId) {

        var currentLevelQuestions = questions.stream()
                .filter(q -> q.level() == levelId)
                .toList();

        if (currentLevelQuestions.isEmpty())
            return null;

        Random rand = new Random();
        return currentLevelQuestions.get(rand.nextInt(currentLevelQuestions.size()));
    }

    // Data Structures and Types

    enum Hint
    {
        None,
        FiftyFifty,
        FriendCall,
        AudienceHelp
    }

    record Question(String text, int level, List<String> choiceList, int answerId) {
    }

    record Level(int id, String preface, String afterword, int reward) {
    }

    // Parsing

    static List<Question> getQuestions() {

        JSONParser parser = new JSONParser();
        Object obj = null;

        try {
            obj = parser.parse(new FileReader("./src/main/resources/questions.json"));
        } catch (IOException | ParseException e) {
            System.out.println("Something went wrong with questions " + e);
        }

        JSONObject jsonObject = (JSONObject) obj;
        assert jsonObject != null;

        JSONArray questions = (JSONArray) jsonObject.get("questions");
        List<Question> questionList = new ArrayList<>();

        for (Object question : questions) {
            JSONObject jsonQuestion = (JSONObject) question;
            String text = (String) jsonQuestion.get("text");
            Long level = (Long) jsonQuestion.get("level");

            JSONArray choiceList = (JSONArray) jsonQuestion.get("choices");
            List<String> choices = new ArrayList<>();

            for (Object choice : choiceList) {
                JSONObject jsonChoice = (JSONObject) choice;
                String choiceStr = (String) jsonChoice.get("choice");

                choices.add(choiceStr);
            }

            Long answerId = (Long) jsonQuestion.get("answerId");

            questionList.add(new Question(text, level.intValue(), choices, answerId.intValue()));
        }

        return Collections.unmodifiableList(questionList);
    }

    static List<Level> getLevels() {

        JSONParser parser = new JSONParser();
        Object obj = null;

        try {
            obj = parser.parse(new FileReader("./src/main/resources/levels.json"));
        } catch (IOException | ParseException e) {
            System.out.println("Something went wrong with levels " + e);
        }

        JSONObject jsonObject = (JSONObject) obj;
        assert jsonObject != null;

        JSONArray levelArray = (JSONArray) jsonObject.get("levels");
        List<Level> levels = new ArrayList<>();

        for (Object level : levelArray) {
            JSONObject jsonLevel = (JSONObject) level;
            Long levelId = (Long) jsonLevel.get("id");
            String preface = (String) jsonLevel.get("preface");
            String afterword = (String) jsonLevel.get("afterword");
            Long reward = (Long) jsonLevel.get("reward");

            levels.add(new Level(levelId.intValue(), preface, afterword, reward.intValue()));
        }

        levels.sort(Comparator.comparing(Level::id));

        return Collections.unmodifiableList(levels);
    }
}