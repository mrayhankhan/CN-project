add_library(common STATIC
  common/serialization.cpp
)

target_include_directories(common PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/common)

add_executable(ingest_server
  server/ingest_server.cpp
  server/normalizer.cpp
  server/broadcaster.cpp
  server/persistence.cpp
  server/admin_http.cpp
)

target_link_libraries(ingest_server PRIVATE common pthread)

add_executable(feed_generator feedgen/feed_generator.cpp)
target_link_libraries(feed_generator PRIVATE common pthread)

add_executable(subscriber_client client/subscriber_client.cpp)
target_link_libraries(subscriber_client PRIVATE common pthread)
