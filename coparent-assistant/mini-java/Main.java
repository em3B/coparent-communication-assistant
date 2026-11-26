
import java.util.Random;

public class Main {

    public static void main(String[] args) {

        String[] urls = {
            "http://neetcode.io/problems/number-of-islands",
            "http://neetcode.io/problems/three-integer-sum",
            "http://neetcode.io/problems/coin-change",
            "http://neetcode.io/problems/anagram-groups",
            "http://neetcode.io/problems/find-target-in-rotated-sorted-array",
            "http://neetcode.io/problems/longest-substring-without-duplicates",
            "http://neetcode.io/problems/course-schedule",
            "http://neetcode.io/problems/products-of-array-discluding-self",
            "http://neetcode.io/problems/merge-intervals",
            "http://neetcode.io/problems/top-k-elements-in-list",
            "http://neetcode.io/problems/clone-graph",
            "http://neetcode.io/problems/longest-consecutive-sequence",
            "http://neetcode.io/problems/combination-target-sum",
            "http://neetcode.io/problems/house-robber",
            "http://neetcode.io/problems/kth-largest-element-in-an-array",
            "http://neetcode.io/problems/rotting-fruit",
            "http://neetcode.io/problems/word-break",
            "http://neetcode.io/problems/insert-new-interval",
            "http://neetcode.io/problems/daily-temperatures",
            "http://neetcode.io/problems/longest-palindromic-substring",
            "http://neetcode.io/problems/count-paths",
            "http://neetcode.io/problems/max-water-container",
            "http://neetcode.io/problems/find-minimum-in-rotated-sorted-array",
            "http://neetcode.io/problems/implement-prefix-tree",
            "http://neetcode.io/problems/longest-repeating-substring-with-replacement",
            "http://neetcode.io/problems/level-order-traversal-of-binary-tree",
            "http://neetcode.io/problems/jump-game",
            "http://neetcode.io/problems/subsets",
            "http://neetcode.io/problems/valid-sudoku",
            "http://neetcode.io/problems/partition-equal-subset-sum",
            "http://neetcode.io/problems/eating-bananas",
            "http://neetcode.io/problems/reorder-linked-list",
            "http://neetcode.io/problems/search-for-word",
            "http://neetcode.io/problems/count-connected-components",
            "http://neetcode.io/problems/longest-increasing-subsequence",
            "http://neetcode.io/problems/decode-ways",
            "http://neetcode.io/problems/spiral-matrix",
            "http://neetcode.io/problems/set-zeroes-in-matrix",
            "http://neetcode.io/problems/rotate-matrix",
            "http://neetcode.io/problems/gas-station",
            "http://neetcode.io/problems/combinations-of-a-phone-number",
            "http://neetcode.io/problems/permutations",
            "http://neetcode.io/problems/target-sum",
            "http://neetcode.io/problems/task-scheduling",
            "http://neetcode.io/problems/time-based-key-value-store",
            "http://neetcode.io/problems/design-word-search-data-structure",
            "http://neetcode.io/problems/find-all-anagrams-in-a-string",
            "http://neetcode.io/problems/pacific-atlantic-water-flow",
            "http://neetcode.io/problems/minimum-size-subarray-sum",
            "http://neetcode.io/problems/accounts-merge"
        };

        Random random = new Random();
        int randomNumber = random.nextInt(urls.length);

        System.out.println("Random NeetCode Medium:");
        System.out.println(urls[randomNumber]);
    }
}
