def minimumLTS = "2.361.4"
def configurations = [
	// Linux 8
    [ platform: "linux", jdk: "8", jenkins: null ],
    // windows 8
    [ platform: "windows", jdk: "11", jenkins: minimumLTS, javaLevel: "11" ],
    // Linux 11
    [ platform: "linux", jdk: "11", jenkins: minimumLTS, javaLevel: "11" ],
]

buildPlugin(configurations: configurations)