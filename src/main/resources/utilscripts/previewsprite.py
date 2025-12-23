

# todo this should be a plugin...
def main():
    hexes_str = input("sprite hexstring (space separated)?")
    #hexes_str = "0xFF 0x89 0x89 0x8F 0x89 0x89 0xFF 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x0"
    print("input = " + hexes_str)
    #print(hex(int("0xc0ff", 16)))
    #print("sprite hex= " + hexes)
    #print(hexes.split(" "))
    
    ints = list(map(lambda e: int(e, 16), hexes_str.split(" ")))
    bins = list(map(lambda e: bin(e), ints))
    hexes = list(map(lambda e: hex(e), ints))
    
    #print(hexes)
    for h in hexes:
        print(h[2:].zfill(2))
    
    #print(ints)
    
    #print(bins)
    for b in bins:
        print(b[2:].zfill(8))
    
if __name__ == "__main__":
    main()