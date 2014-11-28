package xtof;

/**
 * My own implementation of a quicksort that sorts one arrays and reproduce these moves into another array
 * 
 * This is useful when we dont want to create an array of Objects that implement Comparable.
 * 
 * @author xtof
 *
 */
public class QuickSort {
    private float[] numbers;
    private int[] idx;
    private int number;

    public void sort(float[] values, int[] indexes) {
        // check for empty or null array
        if (values ==null || values.length==0){
            return;
        }
        numbers = values;
        idx = indexes;
        number = values.length;
        quicksort(0, number - 1);
    }

    private void quicksort(int low, int high) {
        int i = low, j = high;
        // Get the pivot element from the middle of the list
        float pivot = numbers[low + (high-low)/2];

        // Divide into two lists
        while (i <= j) {
            // If the current value from the left list is smaller then the pivot
            // element then get the next element from the left list
            while (numbers[i] < pivot) {
                i++;
            }
            // If the current value from the right list is larger then the pivot
            // element then get the next element from the right list
            while (numbers[j] > pivot) {
                j--;
            }

            // If we have found a values in the left list which is larger then
            // the pivot element and if we have found a value in the right list
            // which is smaller then the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchange(i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort(low, j);
        if (i < high)
            quicksort(i, high);
    }

    private void exchange(int i, int j) {
        {
            float temp = numbers[i];
            numbers[i] = numbers[j];
            numbers[j] = temp;
        }
        {
            int temp = idx[i];
            idx[i] = idx[j];
            idx[j] = temp;
        }
    }
}
