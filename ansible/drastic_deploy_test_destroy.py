import sys
import ansible_container


def main():
    print(str(sys.argv))
    type = sys.argv[1]
    if type == 'browndog':
        pass
    elif(type == 'drastic'):
        commit = sys.argv[2]



if __name__ == "__main__":
    main()

# Read in a JSON file, then do the right thing..
example1 = {
    'deploy': 'drastic-fedora',
    'commit': '9823uor09902348',
    'tests': [],
}

example2 = {
    'tests': []
}

# First run all of the Brown Dog tests in series

# Pick the latest webhook file and delete others
# Deploy that commit and run swarm via ansible container
# Run DRAS-TIC tests
