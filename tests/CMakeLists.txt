enable_testing()

add_executable(test_serialization test_serialization.cpp)
target_link_libraries(test_serialization PRIVATE common pthread)
add_test(NAME serialization COMMAND test_serialization)

add_executable(test_smoothing test_smoothing.cpp ../src/server/normalizer.cpp)
target_link_libraries(test_smoothing PRIVATE common pthread)
target_include_directories(test_smoothing PRIVATE ${CMAKE_SOURCE_DIR}/src)
add_test(NAME smoothing COMMAND test_smoothing)

add_executable(test_token_bucket test_token_bucket.cpp ../src/server/token_bucket.cpp)
target_link_libraries(test_token_bucket PRIVATE common pthread)
target_include_directories(test_token_bucket PRIVATE ${CMAKE_SOURCE_DIR}/src)
add_test(NAME token_bucket COMMAND test_token_bucket)

add_executable(test_replay test_replay.cpp ../src/server/csv_reader.cpp)
target_link_libraries(test_replay PRIVATE common pthread)
target_include_directories(test_replay PRIVATE ${CMAKE_SOURCE_DIR}/src)
add_test(NAME replay COMMAND test_replay)
