package Modules.TextAI;

import Core.Utils.StringUtil;
import java.util.*;

public class TextAI {

    private final int contextSize;
    private final Random r = new Random();

    public TextAI(int contextSize) {
        this.contextSize = contextSize;
    }

    public WordMap extractModelToWordMap(String model) {
        return extractModelToWordMap(model, null);
    }

    public WordMap extractModelToWordMap(String model, WordMap map) {
        if (map == null)
            map = new WordMap();

        model = prepare(model);
        String[] words = model.split(" ");

        if (words.length > contextSize) {
            for (int i = 0; i < words.length - contextSize; i++) {
                String key = generateWordChain(words, contextSize, i).toLowerCase();
                HashMap<String, ArrayList<Integer>> followingMap = map.computeIfAbsent(key, k -> new HashMap<>());
                followingMap.computeIfAbsent(words[i + contextSize], k -> new ArrayList<>()).add(map.getMessages().size());
            }

            String prefix = generateWordChain(words, contextSize, 0);
            map.getMessages().add(new WordMap.Message(prefix, model));
        }

        return map;
    }

    public Optional<String> generateTextWithWordMap(WordMap wordMap, int maxLength) {
        if (wordMap.getMessages().size() == 0) return Optional.empty();

        for (int j = 0; j < 100; j++) { /* tries 100 times to generate a unique sentence */
            int messageVariety = 0;
            int startIndex = new Random().nextInt(wordMap.getMessages().size());

            String key = wordMap.getMessages().get(startIndex).getPrefix();

            StringBuilder sb = new StringBuilder(key);

            while (sb.length() <= maxLength) {
                if (key.contains("\0")) {
                    break;
                }

                HashMap<String, ArrayList<Integer>> followingMap = wordMap.get(key.toLowerCase());
                if (followingMap == null) break;

                ArrayList<Map.Entry<String, ArrayList<Integer>>> values = new ArrayList<>(followingMap.entrySet());
                int sum = values.stream().mapToInt(entry -> entry.getValue().size()).sum();
                int select = r.nextInt(sum);

                int i = 0;
                while (select >= 0) {
                    Map.Entry<String, ArrayList<Integer>> entry = values.get(i);
                    select -= entry.getValue().size();
                    if (select < 0) {
                        if (contextSize >= 2) {
                            key = key.substring((key).split(" ")[0].length() + 1) + " " + entry.getKey();
                        } else {
                            key = entry.getKey();
                        }

                        if (!entry.getValue().contains(startIndex) && !entry.getKey().contains("\0")) {
                            messageVariety++;
                            startIndex = entry.getValue().get(0);
                        }

                        sb.append(" ").append(entry.getKey());
                        break;
                    }
                    i++;
                }
            }

            /* the generated sentence will be used if it...
            * 1) ...contains at least 30 characters
            * 2) ...doesn't have more than 80% similarity with any real message
            * 3) ...contains different parts from at least two different real messages
            *
            * if not, then the software generates a new sentence
            * until all of the 3 requirements have been matched
            * */

            if (messageVariety >= 1) {
                String result = complete(sb.toString(), 3);
                String finalResult = result.substring(0, result.length() - 2);

                double biggestSimilarity = wordMap.getMessages().stream().mapToDouble(str -> StringUtil.similarityIgnoreLength(str.getSentence(), finalResult)).max().orElse(1.0);
                if (biggestSimilarity < 0.8 && result.length() >= 30) return Optional.of(finalResult);
            }
        }

        return Optional.empty();
    }

    private String prepare(String string) {
        return string.replace("\n\n", "\n")
                .replace("\n", " \n ")
                .replace("  ", "") + " " + "\0";
    }

    private String generateWordChain(String[] words, int wordCount, int start) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < wordCount; i++) {
            if (i > 0) sb.append(" ");
            sb.append(words[start + i]);
        }

        return sb.toString();
    }

    private String complete(String string, int wordCountCheck) {
        String[] words = string.split(" ");
        int validWords = words.length;
        int amount = 1;

        outer : for(int i = 0; i < words.length - wordCountCheck; i++) {
            String key = generateWordChain(words, wordCountCheck, i).toLowerCase();
            for(int j = i + 1; j < words.length - wordCountCheck; j++) {
                String value = generateWordChain(words, wordCountCheck, j).toLowerCase();
                if (key.equals(value)) {
                    amount--;
                    if (amount <= 0) {
                        validWords = j;
                        break outer;
                    }
                }
            }
        }

        return generateWordChain(words, validWords, 0).replace(" \n ", "\n");
    }

    /* word map class */
    public static class WordMap extends HashMap<String, HashMap<String, ArrayList<Integer>>> {

        private final ArrayList<Message> messages = new ArrayList<>();

        public ArrayList<Message> getMessages() {
            return messages;
        }

        /* the saved messages */
        public static class Message {

            private final String prefix, sentence;

            public Message(String prefix, String sentence) {
                this.prefix = prefix;
                this.sentence = sentence;
            }

            public String getPrefix() {
                return prefix;
            }

            public String getSentence() {
                return sentence;
            }

        }

    }

}
