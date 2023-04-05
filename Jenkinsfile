def minimumLTS = "2.332.1"
def configurations = [
	// Linux 8
    [ platform: "linux", jdk: "11", jenkins: null ],
    // windows 8
    [ platform: "windows", jdk: "11", jenkins: minimumLTS, javaLevel: "11" ],
    // Linux 11
    [ platform: "linux", jdk: "11", jenkins: minimumLTS, javaLevel: "11" ],
]

buildPlugin(configurations: configurations)