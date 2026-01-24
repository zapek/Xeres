#  Copyright (c) 2026 by David Gerber - https://zapek.com
#
#  This file is part of Xeres.
#
#  Xeres is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  Xeres is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
#
#  This file is part of Xeres.
#
#  Xeres is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  Xeres is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
#
#  This file is part of Xeres.
#
#  Xeres is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  Xeres is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
#
#  This file is part of Xeres.
#
#  Xeres is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  Xeres is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with Xeres.  If not, see <http://www.gnu.org/licenses/>.

def find_duplicate_lines(filename):
	"""
	Find duplicate lines in property files. Useful for checking internationalization files.

	Args:
		filename (str): Path to the input file

	Returns:
		dict: Dictionary with duplicate values as keys and list of line numbers as values
	"""
	# Dictionary to store values and their line numbers
	values_dict = {}

	try:
		with open(filename, 'r') as file:
			for line_num, line in enumerate(file, 1):
				line = line.strip()

				# Skip empty lines
				if not line:
					continue

				# Find the '=' sign and get the value after it
				if '=' in line:
					# Split on first '=' to get the value part
					parts = line.split('=', 1)
					if len(parts) == 2:
						value = parts[1].strip()

						# Store line numbers for each value
						if value not in values_dict:
							values_dict[value] = []
						values_dict[value].append(line_num)

	except FileNotFoundError:
		print(f"Error: File '{filename}' not found.")
		return {}
	except Exception as e:
		print(f"Error reading file: {e}")
		return {}

	# Filter to only show duplicates (values that appear more than once)
	duplicates = {value: lines for value, lines in values_dict.items() if len(lines) > 1}

	return duplicates


def main():
	# Get filename from user
	filename = input("Enter the filename: ").strip()

	# Find duplicates
	duplicates = find_duplicate_lines(filename)

	# Display results
	if duplicates:
		print("\nDuplicate values found:")
		print("-" * 50)
		for value, line_numbers in duplicates.items():
			print(f"Value: '{value}'")
			print(f"Lines: {', '.join(map(str, line_numbers))}")
			print()
	else:
		print("No duplicate values found.")


if __name__ == "__main__":
	main()
