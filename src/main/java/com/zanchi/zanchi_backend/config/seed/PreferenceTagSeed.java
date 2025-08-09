package com.zanchi.zanchi_backend.config.seed;

import com.zanchi.zanchi_backend.domain.preference.PreferenceTag;
import com.zanchi.zanchi_backend.domain.preference.repository.PreferenceTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class PreferenceTagSeed implements CommandLineRunner {

    private final PreferenceTagRepository repo;

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        List<PreferenceTag> tags = List.of(
                PreferenceTag.builder().code("BALLAD").name("발라드").build(),
                PreferenceTag.builder().code("K_POP").name("K-POP").build(),
                PreferenceTag.builder().code("HIPHOP_RAP").name("힙합/랩").build(),
                PreferenceTag.builder().code("DANCE").name("댄스").build(),
                PreferenceTag.builder().code("POP").name("팝송").build(),
                PreferenceTag.builder().code("OST").name("OST").build(),
                PreferenceTag.builder().code("MUSICAL").name("뮤지컬").build(),
                PreferenceTag.builder().code("EXCITING").name("신나는").build(),
                PreferenceTag.builder().code("CALM").name("잔잔한").build(),
                PreferenceTag.builder().code("EMOTIONAL").name("감성적인").build(),
                PreferenceTag.builder().code("BRIGHT").name("발랄한").build(),
                PreferenceTag.builder().code("INDIE").name("인디").build()
        );
        repo.saveAll(tags);
    }
}
