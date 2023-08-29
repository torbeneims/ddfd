To run the benchmarks:
* Make sure nix is installed. I have instead placed the [nix-portable](https://github.com/DavHau/nix-portable) executable in the home directory. If you have nix installes natively, you can omit all references to the binary and directly start the commnd with `nix ...`
* I have cloned this repository to `~/dfd`.

## Pre-Setup
Gather most required files by running
```bash
nix build gitlab:torbeneims/ddfd-evaluation#all
```
on a nix-enabled system (this does not work with nix-portalbe).
Then copy the files from the `result` link to the `scripts` folder in this repository. Make sure not to overwrite files.

Then run these commands:
## Setup
```bash
cd ~/dfd/scripts/data
# Enter dev shell with required python packages
../nix-portable nix develop .#pyShell # <-- The # before is not a comment
sh ../../make_datasets.sh

# Exit dev shell
^d # or exit
```
In different shells, start the spark master and workers:
```bash
cd ~/dfd; ../nix-portable nix develop .#default -c master & > spark_master.log
cd ~/dfd; sh 8_spark_workers.sh > spark_clients.log
``````
Make sure the workers are only started when necessary, it makes sense to execute the commands in evaluate.sh individually or comment them out

## Evaluation
```bash
# Start a screen
screen -RR -D hyperfine
cd ~/dfd/scripts
# Enter a different dev shell for evaluation
../../nix-portable nix develop ..#default
sh ../evaluate.sh
^d 
```

## Result collection
```bash
cd ~/dfd/scripts; ../../nix-portable nix develop ..#pyShell
sh ../collect_results.sh
```