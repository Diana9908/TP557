class AudioProcessor extends AudioWorkletProcessor {
  constructor(options) {
    super();
    this.sourceSampleRate = options.processorOptions.sourceSampleRate;
    this.targetSampleRate = options.processorOptions.targetSampleRate;
    this.buffer = new Float32Array(1024); 
    this.bufferIndex = 0;
  }

  resampleAudio(inputData, sourceRate, targetRate) {
    const ratio = sourceRate / targetRate;
    const newLength = Math.round(inputData.length / ratio);
    const result = new Float32Array(newLength);
    for (let i = 0; i < newLength; i++) {
      const idx = i * ratio;
      const leftIdx = Math.floor(idx);
      const rightIdx = Math.min(leftIdx + 1, inputData.length - 1);
      const fraction = idx - leftIdx;
      result[i] = inputData[leftIdx] * (1 - fraction) + inputData[rightIdx] * fraction;
    }
    return result;
  }

  process(inputs, outputs, parameters) {
    const input = inputs[0];
    if (input.length > 0) {
      let inputData = input[0];

     
      if (this.sourceSampleRate !== this.targetSampleRate) {
        inputData = this.resampleAudio(inputData, this.sourceSampleRate, this.targetSampleRate);
      }

      
      if (this.bufferIndex + inputData.length <= this.buffer.length) {
        this.buffer.set(inputData, this.bufferIndex);
        this.bufferIndex += inputData.length;
      } else {
        
        this.port.postMessage(this.buffer.slice(0, this.bufferIndex));
        
        this.buffer = new Float32Array(1024);
        this.buffer.set(inputData);
        this.bufferIndex = inputData.length;
      }
    }
    return true;
  }
}

registerProcessor('audio-processor', AudioProcessor);