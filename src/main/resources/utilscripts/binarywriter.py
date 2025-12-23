import sys
import os
#todo this should be a plugin
def write_binary_file(input_file_path):
    """
    Reads a text file containing hexadecimal values, converts them to binary data,
    and writes the data to a new file with a .ch8 extension.
    """
    if not os.path.exists(input_file_path):
        #print(f'{os.path.abspath(input_file_path)=}')
        print(f"Error: Input file not found at '{input_file_path}'")
        return

    try:
        with open(input_file_path, 'r') as f:
            hex_string = f.read()
    except IOError as e:
        print(f"Error reading input file: {e}")
        return

    # Remove all whitespace (spaces, newlines, tabs)
    cleaned_hex_string = "".join(hex_string.split())
    # Remove Ox prefixes
    cleaned_hex_string = cleaned_hex_string.replace("0x", "")

    # Ensure the hex string has an even number of characters
    if len(cleaned_hex_string) % 2 != 0:
        print("Error: The hex string must have an even number of characters.")
        return
        
    try:
        binary_data = bytes.fromhex(cleaned_hex_string)
    except ValueError as e:
        print(f"Error converting hex to binary: {e}")
        return

    # Create the output file path
    base_name = os.path.splitext(input_file_path)[0]
    output_file_path = base_name + ".ch8"

    try:
        with open(output_file_path, 'wb') as f:
            f.write(binary_data)
        print(f"Successfully wrote {len(binary_data)} bytes to '{output_file_path}'")
    except IOError as e:
        print(f"Error writing to output file: {e}")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python binarywriter.py <input_file_path>")
    else:
        write_binary_file(sys.argv[1])