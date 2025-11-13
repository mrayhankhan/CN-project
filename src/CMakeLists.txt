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
  server/token_bucket.cpp
)

target_link_libraries(ingest_server PRIVATE common pthread)

add_executable(replay_server
  server/replay_server.cpp
  server/csv_reader.cpp
  server/broadcaster.cpp
  server/token_bucket.cpp
)

target_link_libraries(replay_server PRIVATE common pthread)

add_executable(feed_generator feedgen/feed_generator.cpp)
target_link_libraries(feed_generator PRIVATE common pthread)

add_executable(subscriber_client client/subscriber_client.cpp)
target_link_libraries(subscriber_client PRIVATE common pthread)

add_executable(websocket_bridge
  server/websocket_bridge.cpp
)

target_link_libraries(websocket_bridge PRIVATE pthread ssl crypto)
