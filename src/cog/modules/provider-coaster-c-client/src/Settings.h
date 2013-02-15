/*
 * ServiceSettings.h
 *
 *  Created on: Sep 9, 2012
 *      Author: mike
 */

#ifndef SETTINGS_H_
#define SETTINGS_H_

#include <string>
#include <map>
#include <iostream>

using namespace std;

class Settings {
	private:
		map<string*, const char*> settings;
	public:
		Settings();
		virtual ~Settings();
		void set(string& key, string& value);
		void set(string& key, const char* value);
		void remove(string& key);
		map<string*, const char*>* getSettings();

		template<typename cls> friend cls& operator<< (cls& os, Settings& s);

		class Key {
			public:
				static string SLOTS;
				static string JOBS_PER_NODE;
				static string NODE_GRANULARITY;
				static string ALLOCATION_STEP_SIZE;
				static string MAX_NODES;
				static string LOW_OVERALLOCATION;
				static string HIGH_OVERALLOCATION;
				static string OVERALLOCATION_DECAY_FACTOR;
				static string SPREAD;
				static string RESERVE;
				static string MAXTIME;
				static string REMOTE_MONITOR_ENABLED;
				static string INTERNAL_HOSTNAME;
				static string WORKER_MANAGER;
				static string WORKER_LOGGING_LEVEL;
				static string WORKER_LOGGING_DIRECTORY;
				static string LD_LIBRARY_PATH;
				static string WORKER_COPIES;
				static string DIRECTORY;
				static string USE_HASH_BANG;
				static string PARALLELISM;
				static string CORES_PER_NODE;
		};
};

template<typename cls> cls& operator<< (cls& os, Settings& s) {
	os << "Settings(";
	map<string*, const char*>* m = s.getSettings();
	map<string*, const char*>::iterator i;

	for (i = m->begin(); i != m->end(); i++) {
		os << i->first << ": " << i->second;
		if (i != --m->end()) {
			os << ", ";
		}
	}
	os << ")";
	return os;
}

#endif /* SETTINGS_H_ */
