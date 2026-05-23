import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestGroq {
    public static void main(String[] args) throws Exception {
        String apiKey = "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";
        String prompt = "Responde APENAS com um JSON array com 1 elemento, sem markdown:\n" +
                        "[\n" +
                        "  {\n" +
                        "    \"name\": \"Nome do outfit\",\n" +
                        "    \"description\": \"Descrição curta do look\",\n" +
                        "    \"itemIds\": [1, 4, 7],\n" +
                        "    \"weatherNote\": \"Perfeito para este frio\"\n" +
                        "  }\n" +
                        "]";
        
        String body = "{" +
                "\"model\": \"llama-3.3-70b-versatile\"," +
                "\"temperature\": 0.4," +
                "\"messages\": [{\"role\": \"user\", \"content\": \"" + prompt.replace("\n", "\\n").replace("\"", "\\\"") + "\"}]" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
    }
}
