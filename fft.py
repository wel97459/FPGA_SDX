import numpy as np
import csv
import matplotlib.pyplot as plt

# Read in the CSV file
filename = "output.csv"
data = []
with open(filename, 'r') as csvfile:
    csvreader = csv.reader(csvfile)
    for row in csvreader:
        data.append(float(row[0]))

# Compute the FFT of the data using NumPy
fft = np.fft.fft(data)

# Compute the power spectrum
power_spectrum = np.abs(fft+0.001)**2

# Convert power spectrum to dBm
reference_power = 1e-3  # 1 milliwatt
impedance = 50         # 50 ohms
power_spectrum_dbm = 10 * np.log10(power_spectrum*1000/impedance/reference_power)

# Calculate the frequency range of the power spectrum
sample_rate = 30e6     # 100 MHz
freq_range = np.fft.fftfreq(len(power_spectrum), 1/sample_rate)

# Plot the power spectrum
plt.plot(freq_range, power_spectrum_dbm)
plt.xlabel('Frequency (MHz)')
plt.ylabel('Power (dBm)')
plt.title('Power Spectrum of ' + filename)

# Set the x-axis limits to a specific frequency range
#plt.xlim(0, sample_rate/2)

plt.show()