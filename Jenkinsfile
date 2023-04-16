def minimumLTS = "2.204.6"
def configurations = [
	// Linux 8
    [ platform: "linux", jdk: "8", jenkins: null ],
    // windows 8
    [ platform: "windows", jdk: "11", jenkins: minimumLTS ],
    // Linux 11
    [ platform: "linux", jdk: "11", jenkins: minimumLTS],
]

buildPlugin(configurations: configurations)