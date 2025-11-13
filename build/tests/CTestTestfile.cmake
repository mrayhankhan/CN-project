# CMake generated Testfile for 
# Source directory: /workspaces/CN-project/tests
# Build directory: /workspaces/CN-project/build/tests
# 
# This file includes the relevant testing commands required for 
# testing this directory and lists subdirectories to be tested as well.
add_test(serialization "/workspaces/CN-project/build/tests/test_serialization")
set_tests_properties(serialization PROPERTIES  _BACKTRACE_TRIPLES "/workspaces/CN-project/tests/CMakeLists.txt;5;add_test;/workspaces/CN-project/tests/CMakeLists.txt;0;")
add_test(smoothing "/workspaces/CN-project/build/tests/test_smoothing")
set_tests_properties(smoothing PROPERTIES  _BACKTRACE_TRIPLES "/workspaces/CN-project/tests/CMakeLists.txt;10;add_test;/workspaces/CN-project/tests/CMakeLists.txt;0;")
add_test(token_bucket "/workspaces/CN-project/build/tests/test_token_bucket")
set_tests_properties(token_bucket PROPERTIES  _BACKTRACE_TRIPLES "/workspaces/CN-project/tests/CMakeLists.txt;15;add_test;/workspaces/CN-project/tests/CMakeLists.txt;0;")
add_test(replay "/workspaces/CN-project/build/tests/test_replay")
set_tests_properties(replay PROPERTIES  _BACKTRACE_TRIPLES "/workspaces/CN-project/tests/CMakeLists.txt;20;add_test;/workspaces/CN-project/tests/CMakeLists.txt;0;")
