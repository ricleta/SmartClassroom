import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:smartclassroom_ble_plugin/smartclassroom_ble_plugin.dart';

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
  final _smartclassroomBlePlugin = SmartclassroomBlePlugin();

  @override
  void initState() {
    super.initState();
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
            ],
          ),
        ),
      ),
    );
  }
}
