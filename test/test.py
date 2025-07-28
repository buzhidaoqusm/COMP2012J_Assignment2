import os

os.system("javac -nowarn ./1/*.java")
os.system("javac -nowarn ./2/*.java")

Algorithms = ["LRU", "Clock_policy"]
print("replacementAlgorithm(0: All, 1: RLU, 2: Clock_policy):")
choice = int(input())
print("Number of processes:")
num_processes = int(input())

command_file = "commands"

confs = []
commands = []

def read_file(file_name, list_name):
    with open(file_name, "r") as f:
        for line in f:
            list_name.append(line.strip())

read_file(command_file, commands)

def compare_file(s, c):
    with open("1" + s + c, 'r') as of, open("2" + s + c, 'r') as tf:
            of_lines = of.readlines()
            tf_lines = tf.readlines()
            if len(of_lines) != len(tf_lines):
                return 0
            for i in range(len(of_lines)):
                if of_lines[i] != tf_lines[i]:
                    return 0
    return 1

for command in commands:
    if (choice == 0 or choice == 1):
        with open("./1/conf_LRU_" + command, "w") as f:
            f.write("enable_logging true\n")
            f.write("log_file 1LRU_" + command + "\n")
            f.write("pagesize 16384\n");
            f.write("addressradix 16\n");
            f.write("numpages 64\n");
            f.write("replacementAlgorithm " + "LRU" + "\n");
            f.write("physicalMemSize " + str(num_processes) + "\n");
            for i in range(num_processes):
                f.write("memset " + str(i) + " " + str(i) + " 0 0 0 0" + "\n") 
        with open("./2/conf_LRU_" + command, "w") as f:
            f.write("enable_logging true\n")
            f.write("log_file 2LRU_" + command + "\n")
            f.write("pagesize 16384\n");
            f.write("addressradix 16\n");
            f.write("numpages 64\n");
            f.write("replacementAlgorithm " + "LRU" + "\n");
            f.write("physicalMemSize " + str(num_processes) + "\n");
            for i in range(num_processes):
                f.write("memset " + str(i) + " " + str(i) + " 0 0 0 0" + "\n") 
        print("MemoryManagement1 processing " + command + " with LRU")
        os.system("java -cp ./1 MemoryManagement " + command + " " + "./1/conf_LRU_" + command)
        print("MemoryManagement2 processing " + command + " with LRU")
        os.system("java -cp ./2 MemoryManagement " + command + " " + "./2/conf_LRU_" + command)

        compare = compare_file("LRU_", command)
        if compare == 1:
            print("Both files are the same")
        else:
            print("Different lines in the output files")
            break

    if (choice == 0 or choice == 2):
        with open("./1/conf_Clock_" + command, "w") as f:
            f.write("enable_logging true\n")
            f.write("log_file 1Clock_" + command + "\n")
            f.write("pagesize 16384\n");
            f.write("addressradix 16\n");
            f.write("numpages 64\n");
            f.write("replacementAlgorithm " + "Clock_policy" + "\n");
            f.write("physicalMemSize " + str(num_processes) + "\n");
            for i in range(num_processes):
                f.write("memset " + str(i) + " " + str(i) + " 0 0 0 0" + "\n") 
        with open("./2/conf_Clock_" + command, "w") as f:
            f.write("enable_logging true\n")
            f.write("log_file 2Clock_" + command + "\n")
            f.write("pagesize 16384\n");
            f.write("addressradix 16\n");
            f.write("numpages 64\n");
            f.write("replacementAlgorithm " + "Clock_policy" + "\n");
            f.write("physicalMemSize " + str(num_processes) + "\n");
            for i in range(num_processes):
                f.write("memset " + str(i) + " " + str(i) + " 0 0 0 0" + "\n") 
        print("MemoryManagement processing " + command + " with Clock_policy")
        os.system("java -cp ./1 MemoryManagement " + command + " " + "./1/conf_Clock_" + command)
        print("MemoryManagement processing " + command + " with Clock_policy")
        os.system("java -cp ./2 MemoryManagement " + command + " " + "./2/conf_Clock_" + command)

        compare = compare_file("Clock_", command)
        if compare == 1:
            print("Both files are the same")
        else:
            print("Different lines in the output files")
            break
    
