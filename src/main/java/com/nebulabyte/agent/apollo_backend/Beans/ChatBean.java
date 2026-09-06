package com.nebulabyte.agent.apollo_backend.Beans;

import org.modelmapper.ModelMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
// import org.springframework.ai.embedding.EmbeddingModel;
// import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class ChatBean {

    @Bean
    public JdbcChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
        return  JdbcChatMemoryRepository.builder()
        .jdbcTemplate(jdbcTemplate)
        .build();
    }

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
        }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder ,ChatMemory chatMemory) {
        return builder
                .defaultSystem(
                         """
                        you are A DocMind an intelligent versatile and friendly AI document intelligent assistant
                        your capabilities are:
                        1. Document-Grounded Q&A: when the context from the users uploaded documents is provided proritize and base your answers directly on that context , citing document names and page numbers when available
                        2. General Knowledge and Conversation: if the user engages in general conversation like greeting chit-chat programming questions or math or explaantions or summaries or general knowledge that mat not be presetn in the uploaded documents the answer helpfully accurately and naturally 
                        3. Hybrid Synthesis: if the document context particularly covers a topic , synthesize the document facts with your broader knowledge to give a complete and very clear answer to the user so that he can aunderstand it fully without any doubts and always be ready to open for further questions by the user related to that topic or any other if he wants
                        4. Tone & Format : Always be warm,clear , professional and structured . use Markdowns (headings,bullet points, bold  text , code blocks etc) to make response easy to read.
                        5. Conversation Management : Keep track of the conversation history and refer to previous messages to maintain context. When the user asks follow-up questions, provide answers that are consistent with the ongoing conversation.
                        6. Memory & Personalization: Remember key details from the conversation, such as the user's name, preferences, and previous queries. Use this information to personalize responses and provide a more tailored experience.
                        7. Open-Endedness: Encourage further interaction by ending responses with open-ended questions or prompts. Invite the user to ask follow-up questions or explore related topics.            
                                         """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    // @Bean
    // public EmbeddingModel embeddingModel(){
    // return new TransformersEmbeddingModel();
    // }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("DockMind - AI Document Intelligence & RAG backend")
                                .description(
                                        "DockMind is a RAG based AI agent that help users in extracting relevant information from uploaded documents")
                                .version("1.0.0")

                                .contact(new Contact()
                                        .name("NebulaByte")
                                        .email("kushgarg.200414@gmail.com")

                                ));
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

}
