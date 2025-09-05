import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:smartclassroom_ble_plugin/smartclassroom_ble_plugin.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String studentId = '';
  List<String> receivedMessages = [];
  final _smartclassroomBlePlugin = SmartclassroomBlePlugin();
  final ScrollController _scrollController = ScrollController(); // Controller for auto-scrolling

  @override
  void initState() {
    super.initState();
    _requestPermissions();
    _smartclassroomBlePlugin.messageStream.listen((message) {
      // ignore: avoid_print
      print('Received message: $message');
      if (message.isNotEmpty && mounted) {
        setState(() {
          receivedMessages.add(message);
        });
        // Add this part to scroll down
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (_scrollController.hasClients) {
            _scrollController.animateTo(
              _scrollController.position.maxScrollExtent,
              duration: const Duration(milliseconds: 300),
              curve: Curves.easeOut,
            );
          }
        });
      }
    });
  }

  @override
  void dispose() {
    _scrollController.dispose(); // Dispose the controller
    super.dispose();
  }

  Future<void> _requestPermissions() async {
    await [Permission.bluetoothScan, Permission.bluetoothAdvertise, Permission.bluetoothConnect].request();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Plugin example app')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: TextField(
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: 'Student ID',
                  ),
                  onChanged: (text) {
                    setState(() {
                      studentId = text;
                    });
                  },
                ),
              ),
              ElevatedButton(
                onPressed: () {
                  // Call method to start advertising with the student ID
                  _smartclassroomBlePlugin.startAdvertising(studentId);
                },
                child: const Text('Start Advertising'),
              ),
              ElevatedButton(
                onPressed: () {
                  // Call method to stop advertising
                  _smartclassroomBlePlugin.stopAdvertising();
                },
                child: const Text('Stop Advertising'),
              ),
              ElevatedButton(
                onPressed: () {
                  // Call method to start listening
                  _smartclassroomBlePlugin.startListening();
                },
                child: const Text('Start Listening'),
              ),
              ElevatedButton(
                onPressed: () {
                  // Call method to stop listening
                  _smartclassroomBlePlugin.stopListening();
                },
                child: const Text('Stop Listening'),
              ),
              const SizedBox(height: 20),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text(
                            'Received Messages:',
                            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                          ),
                          IconButton(
                            icon: const Icon(Icons.delete_outline, color: Colors.blueAccent),
                            tooltip: 'Clear messages',
                            onPressed: () {
                              setState(() {
                                receivedMessages.clear();
                              });
                            },
                          ),
                        ],
                      ),
                    ),
                    Expanded(
                      child: Container(
                        margin: const EdgeInsets.all(8.0),
                        decoration: BoxDecoration(
                          border: Border.all(color: Colors.blueAccent),
                          borderRadius: BorderRadius.circular(8.0),
                        ),
                        child: receivedMessages.isEmpty
                            ? const Center(
                                child: Text('No messages received yet.'),
                              )
                            : ListView.builder(
                                controller: _scrollController, // Assign the controller
                                itemCount: receivedMessages.length,
                                itemBuilder: (context, index) {
                                  return ListTile(
                                    title: Text(receivedMessages[index]),
                                  );
                                },
                              ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
