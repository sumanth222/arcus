package com.arcus.arc1.test;

import com.arcus.arc1.levelDeterminer.LevelDeterminer;
import com.arcus.arc1.levelDeterminer.LevelDeterminerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LevelDeterminerTest {

    private LevelDeterminer levelDeterminer;

    @BeforeEach
    void setup(){
        levelDeterminer = new LevelDeterminer();
    }

    @ParameterizedTest
    @CsvSource({
            "2,1,5,beginner",
            "30,30,40,advanced",
            "50,50,50,expert",
            "10,10,10,medium"
    })
    void testLevel(int push, int pull, int sit, String level){
        LevelDeterminerDTO ldd = new LevelDeterminerDTO(push, pull, sit);
        assertEquals(level, levelDeterminer.determineLevel(ldd));
    }
}
